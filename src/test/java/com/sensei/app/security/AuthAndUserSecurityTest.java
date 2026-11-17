package com.sensei.app.security;

import com.sensei.app.config.SecurityConfig;
import com.sensei.app.controller.AuthController;
import com.sensei.app.controller.UserController;
import com.sensei.app.enums.UserRole;
import com.sensei.app.model.User;
import com.sensei.app.repository.ClassRepository;
import com.sensei.app.repository.MaterialRepository;
import com.sensei.app.repository.UserRepository;
import com.sensei.app.security.jwt.AuthEntryPointJwt;
import com.sensei.app.security.jwt.JwtTokenProvider;
import com.sensei.app.service.AuthService;
import com.sensei.app.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de segurança com a configuração real (SecurityFilterChain, filtro JWT,
 * BCrypt, AuthenticationManager, handlers 401/403 e method security).
 * Somente os repositories do MongoDB são simulados: nenhum banco ou serviço externo é usado.
 */
@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({
        SecurityConfig.class,
        AuthEntryPointJwt.class,
        JsonAccessDeniedHandler.class,
        JwtTokenProvider.class,
        UserDetailsServiceImpl.class,
        AuthService.class,
        UserService.class
})
@TestPropertySource(properties = {
        "app.jwtSecret=" + AuthAndUserSecurityTest.TEST_SECRET,
        "app.jwtExpirationMs=3600000",
        "app.cors.allowed-origins=http://localhost:5173"
})
class AuthAndUserSecurityTest {
    // Valor exclusivo de teste (>= 32 bytes). Não é usado em nenhum ambiente.
    static final String TEST_SECRET = "test-only-jwt-secret-for-automated-tests-0123456789";
    private static final String RAW_PASSWORD = "senhaSegura123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ClassRepository classRepository;

    @MockitoBean
    private MaterialRepository materialRepository;

    @BeforeEach
    void assertNoLeakedInvocations() {
        // Nenhum mock pode chegar a um teste com invocações de testes anteriores.
        for (Object mock : List.of(userRepository, classRepository, materialRepository)) {
            assertThat(Mockito.mockingDetails(mock).getInvocations())
                    .as("invocations leaked from a previous test").isEmpty();
        }
    }

    // ---------- helpers ----------

    private User persistedUser(String id, String email, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName("Usuário " + id);
        user.setEmail(email);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private String bearer(User user) {
        AuthenticatedUser principal = AuthenticatedUser.from(user);
        String token = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return "Bearer " + token;
    }

    private void stubSaveAssigningId(String id) {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(id);
            }
            return user;
        });
    }

    private ResultActions register(String json) throws Exception {
        return mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    // ---------- cadastro ----------

    @Nested
    class Register {
        @Test
        void studentCanRegisterPubliclyAndPasswordIsStoredWithBcrypt() throws Exception {
            when(userRepository.findByEmail("aluno@test.com")).thenReturn(Optional.empty());
            stubSaveAssigningId("new-student");

            register("""
                    {"name":"Aluno","email":"aluno@test.com","role":"STUDENT","password":"senhaSegura123"}
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("new-student"))
                    .andExpect(jsonPath("$.email").value("aluno@test.com"))
                    .andExpect(jsonPath("$.role").value("STUDENT"))
                    .andExpect(jsonPath("$.password").doesNotExist());

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            String storedHash = saved.getValue().getPassword();
            assertThat(storedHash).isNotEqualTo("senhaSegura123").startsWith("$2");
            assertThat(passwordEncoder.matches("senhaSegura123", storedHash)).isTrue();
        }

        @Test
        void teacherCanRegisterPublicly() throws Exception {
            when(userRepository.findByEmail("prof@test.com")).thenReturn(Optional.empty());
            stubSaveAssigningId("new-teacher");

            register("""
                    {"name":"Professora","email":"prof@test.com","role":"TEACHER","password":"senhaSegura123"}
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("TEACHER"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        void invalidRoleIsRejected() throws Exception {
            register("""
                    {"name":"X","email":"x@test.com","role":"ADMIN","password":"senhaSegura123"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("TEACHER")));

            verify(userRepository, never()).save(any());
        }

        @Test
        void missingRoleIsRejected() throws Exception {
            register("""
                    {"name":"X","email":"x@test.com","password":"senhaSegura123"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.role").value("Role is required."));

            verify(userRepository, never()).save(any());
        }

        @Test
        void duplicateEmailReturnsConflict() throws Exception {
            persistedUser("u1", "existente@test.com", UserRole.STUDENT);

            register("""
                    {"name":"Outro","email":"existente@test.com","role":"STUDENT","password":"senhaSegura123"}
                    """)
                    .andExpect(status().isConflict());

            verify(userRepository, never()).save(any());
        }

        @Test
        void tooShortPasswordIsRejected() throws Exception {
            register("""
                    {"name":"X","email":"x@test.com","role":"STUDENT","password":"123"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());

            verify(userRepository, never()).save(any());
        }

        @Test
        void passwordLongerThan72BytesIsRejectedInsteadOfFailingInBcrypt() throws Exception {
            // 40 caracteres de 2 bytes em UTF-8 = 80 bytes (dentro do limite de 72 caracteres do DTO).
            String password = "é".repeat(40);
            when(userRepository.findByEmail("x@test.com")).thenReturn(Optional.empty());

            register("{\"name\":\"X\",\"email\":\"x@test.com\",\"role\":\"STUDENT\",\"password\":\"" + password + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("72 bytes")));

            verify(userRepository, never()).save(any());
        }

        @Test
        void legacyCreateEndpointNoLongerExists() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            mvc.perform(post("/user/create").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"X\",\"email\":\"x@test.com\",\"role\":\"TEACHER\",\"password\":\"senhaSegura123\"}"))
                    .andExpect(status().is4xxClientError());

            verify(userRepository, never()).save(any());
        }
    }

    // ---------- login ----------

    @Nested
    class Login {
        @Test
        void validCredentialsReturnBearerToken() throws Exception {
            persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            MvcResult result = login("aluno@test.com", RAW_PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andReturn();

            String token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("aluno@test.com");
        }

        @Test
        void wrongPasswordAndUnknownEmailReturnTheSameGeneric401() throws Exception {
            persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            when(userRepository.findByEmail("naoexiste@test.com")).thenReturn(Optional.empty());

            String wrongPassword = login("aluno@test.com", "senhaErrada999")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value(SecurityErrorResponses.INVALID_CREDENTIALS_MESSAGE))
                    .andReturn().getResponse().getContentAsString();

            String unknownEmail = login("naoexiste@test.com", "senhaErrada999")
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();

            assertThat(unknownEmail).isEqualTo(wrongPassword);
            assertThat(wrongPassword).doesNotContain("aluno@test.com").doesNotContain("$2");
        }

        @Test
        void loginWithPasswordLongerThan72BytesReturns401NotServerError() throws Exception {
            persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            // 40 caracteres de 2 bytes em UTF-8 = 80 bytes (acima do limite do BCrypt).
            login("aluno@test.com", "é".repeat(40))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(SecurityErrorResponses.INVALID_CREDENTIALS_MESSAGE));
        }
    }

    // ---------- autenticação por token ----------

    @Nested
    class TokenAuthentication {
        @Test
        void requestWithoutTokenReturns401Json() throws Exception {
            mvc.perform(get("/user/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value(SecurityErrorResponses.UNAUTHENTICATED_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/user/me"));
        }

        @Test
        void malformedTokenReturns401() throws Exception {
            mvc.perform(get("/user/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tamperedTokenReturns401() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            persistedUser("t1", "prof@test.com", UserRole.TEACHER);
            String[] parts = bearer(student).substring("Bearer ".length()).split("\\.");
            String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"sub\":\"prof@test.com\"}".getBytes(StandardCharsets.UTF_8));

            mvc.perform(get("/user/me").header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + parts[0] + "." + forgedPayload + "." + parts[2]))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void expiredTokenReturns401() throws Exception {
            persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            Date past = new Date(System.currentTimeMillis() - 60_000);
            String expired = Jwts.builder()
                    .subject("aluno@test.com")
                    .issuedAt(new Date(past.getTime() - 60_000))
                    .expiration(past)
                    .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                    .compact();

            mvc.perform(get("/user/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(SecurityErrorResponses.UNAUTHENTICATED_MESSAGE));
        }

        @Test
        void validTokenOfDeletedUserReturns401() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            String token = bearer(student);
            when(userRepository.findByEmail("aluno@test.com")).thenReturn(Optional.empty());

            mvc.perform(get("/user/me").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void validTokenReturnsOwnProfileWithoutPassword() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            mvc.perform(get("/user/me").header(HttpHeaders.AUTHORIZATION, bearer(student)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("s1"))
                    .andExpect(jsonPath("$.role").value("STUDENT"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
    }

    // ---------- autorização sobre usuários ----------

    @Nested
    class UserAuthorization {
        @Test
        void studentCannotListUsers_403Json() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            mvc.perform(get("/user/").header(HttpHeaders.AUTHORIZATION, bearer(student)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(SecurityErrorResponses.ACCESS_DENIED_MESSAGE));

            verify(userRepository, never()).findAll();
        }

        @Test
        void teacherListsUsersWithoutPasswordOrHash() throws Exception {
            User teacher = persistedUser("t1", "prof@test.com", UserRole.TEACHER);
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            when(userRepository.findAll()).thenReturn(List.of(teacher, student));

            String body = mvc.perform(get("/user/").header(HttpHeaders.AUTHORIZATION, bearer(teacher)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].password").doesNotExist())
                    .andExpect(jsonPath("$[1].password").doesNotExist())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("$2a$").doesNotContain("$2b$").doesNotContain("password");
        }

        @Test
        void userCannotUpdateAnotherUser_403() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            persistedUser("s2", "outro@test.com", UserRole.STUDENT);

            mvc.perform(patch("/user/update/s2").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Invasor\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(SecurityErrorResponses.ACCESS_DENIED_MESSAGE));

            verify(userRepository, never()).save(any());
        }

        @Test
        void updatingNonExistentForeignIdIsAlso403_noExistenceLeak() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            mvc.perform(patch("/user/update/does-not-exist").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void userCannotDeleteAnotherUser_403() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            persistedUser("t1", "prof@test.com", UserRole.TEACHER);

            mvc.perform(delete("/user/delete/t1").header(HttpHeaders.AUTHORIZATION, bearer(student)))
                    .andExpect(status().isForbidden());

            verify(userRepository, never()).deleteById(anyString());
        }

        @Test
        void roleCannotBeChangedThroughUpdate() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            mvc.perform(patch("/user/update/s1").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TEACHER\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("role")));

            verify(userRepository, never()).save(any());
            assertThat(student.getRole()).isEqualTo(UserRole.STUDENT);
        }

        @Test
        void internalFieldsCannotBeChangedThroughUpdate() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            mvc.perform(patch("/user/update/s1").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Novo\",\"id\":\"outro\",\"password\":\"x\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("id")))
                    .andExpect(jsonPath("$.message", containsString("password")));

            verify(userRepository, never()).save(any());
        }

        @Test
        void partialUpdateOfNameOnlyKeepsEmail() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            stubSaveAssigningId("s1");

            mvc.perform(patch("/user/update/s1").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nome Novo\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Nome Novo"))
                    .andExpect(jsonPath("$.email").value("aluno@test.com"))
                    .andExpect(jsonPath("$.role").value("STUDENT"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        void partialUpdateOfEmailOnly() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            when(userRepository.findByEmail("novo@test.com")).thenReturn(Optional.empty());
            stubSaveAssigningId("s1");

            mvc.perform(patch("/user/update/s1").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"novo@test.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("novo@test.com"))
                    .andExpect(jsonPath("$.name").value("Usuário s1"));
        }

        @Test
        void emptyUpdateReturns400() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            mvc.perform(patch("/user/update/s1").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).save(any());
        }

        @Test
        void updateToAnEmailInUseReturns409() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);
            persistedUser("s2", "ocupado@test.com", UserRole.STUDENT);

            mvc.perform(patch("/user/update/s1").header(HttpHeaders.AUTHORIZATION, bearer(student))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ocupado@test.com\"}"))
                    .andExpect(status().isConflict());

            verify(userRepository, never()).save(any());
        }

        @Test
        void userCanDeleteOwnUnreferencedAccount() throws Exception {
            User student = persistedUser("s1", "aluno@test.com", UserRole.STUDENT);

            mvc.perform(delete("/user/delete/s1").header(HttpHeaders.AUTHORIZATION, bearer(student)))
                    .andExpect(status().isOk());

            verify(userRepository).deleteById("s1");
        }

        @Test
        void referencedAccountIsNotDeleted_409() throws Exception {
            User teacher = persistedUser("t1", "prof@test.com", UserRole.TEACHER);
            when(classRepository.existsByTeacherId("t1")).thenReturn(true);

            mvc.perform(delete("/user/delete/t1").header(HttpHeaders.AUTHORIZATION, bearer(teacher)))
                    .andExpect(status().isConflict());

            verify(userRepository, never()).deleteById(anyString());
        }
    }

    // ---------- CORS ----------

    @Nested
    class Cors {
        @Test
        void preflightFromConfiguredOriginIsAllowed() throws Exception {
            mvc.perform(options("/auth/login")
                            .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        }

        @Test
        void preflightFromUnknownOriginIsRejected() throws Exception {
            mvc.perform(options("/auth/login")
                            .header(HttpHeaders.ORIGIN, "http://unknown-origin.test")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }
}
