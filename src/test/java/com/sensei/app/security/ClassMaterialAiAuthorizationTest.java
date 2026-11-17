package com.sensei.app.security;

import com.sensei.app.config.SecurityConfig;
import com.sensei.app.controller.ClassController;
import com.sensei.app.controller.MaterialController;
import com.sensei.app.controller.OpenAiController;
import com.sensei.app.enums.MaterialStatus;
import com.sensei.app.enums.MaterialType;
import com.sensei.app.enums.UserRole;
import com.sensei.app.model.Class;
import com.sensei.app.model.Material;
import com.sensei.app.model.User;
import com.sensei.app.repository.ClassRepository;
import com.sensei.app.repository.MaterialRepository;
import com.sensei.app.repository.UserRepository;
import com.sensei.app.security.jwt.AuthEntryPointJwt;
import com.sensei.app.security.jwt.JwtTokenProvider;
import com.sensei.app.service.ClassService;
import com.sensei.app.service.MaterialService;
import com.sensei.app.service.OpenAiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorização de turmas, materiais e IA com a configuração de segurança real.
 * Repositories do MongoDB são simulados com Mockito e as chamadas HTTP à OpenAI são
 * interceptadas por MockRestServiceServer: nenhuma requisição externa é feita, e qualquer
 * chamada não esperada faz o teste falhar.
 *
 * Cenário:
 * - t1 (professor) é dono de c1 (aluno s1, materiais m1 WAITING e m2 APPROVED) e de c3 (vazia);
 * - t2 (professor) é dono de c2 (aluno s2, material m3 WAITING).
 */
@WebMvcTest(controllers = {ClassController.class, MaterialController.class, OpenAiController.class})
@Import({
        SecurityConfig.class,
        AuthEntryPointJwt.class,
        JsonAccessDeniedHandler.class,
        JwtTokenProvider.class,
        UserDetailsServiceImpl.class,
        ClassService.class,
        MaterialService.class,
        OpenAiService.class,
        ClassMaterialAiAuthorizationTest.OpenAiTestClient.class
})
@TestPropertySource(properties = {
        "app.jwtSecret=test-only-jwt-secret-for-automated-tests-0123456789",
        "app.jwtExpirationMs=3600000",
        "openai.api.key=test-only-key-not-a-real-credential",
        "openai.api.url=" + ClassMaterialAiAuthorizationTest.TRANSCRIBE_URL,
        "openai.api.chat-url=" + ClassMaterialAiAuthorizationTest.CHAT_URL
})
class ClassMaterialAiAuthorizationTest {
    static final String TRANSCRIBE_URL = "https://openai.test/v1/audio/transcriptions";
    static final String CHAT_URL = "https://openai.test/v1/chat/completions";
    private static final String FAKE_HASH = "$2a$10$fakeHashForTestsOnlyNotUsedForLoginXXXXXXXXXXXXXXXXXXXX";

    @TestConfiguration
    static class OpenAiTestClient {
        @Bean
        RestTemplate openAiRestTemplate() {
            return new RestTemplate();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RestTemplate openAiRestTemplate;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ClassRepository classRepository;

    @MockitoBean
    private MaterialRepository materialRepository;

    private MockRestServiceServer openAi;

    private User t1;
    private User t2;
    private User s1;
    private User s2;
    private Class c1;
    private Class c2;
    private Class c3;
    private Material m1;
    private Material m2;
    private Material m3;

    @BeforeEach
    void setUp() {
        // Reset explícito: garante testes independentes da ordem de execução das classes @Nested.
        Mockito.reset(userRepository, classRepository, materialRepository);

        openAi = MockRestServiceServer.bindTo(openAiRestTemplate).build();

        t1 = user("t1", "prof1@test.com", UserRole.TEACHER);
        t2 = user("t2", "prof2@test.com", UserRole.TEACHER);
        s1 = user("s1", "aluno1@test.com", UserRole.STUDENT);
        s2 = user("s2", "aluno2@test.com", UserRole.STUDENT);

        c1 = clazz("c1", "t1", List.of("s1"), List.of("m1", "m2"));
        c2 = clazz("c2", "t2", List.of("s2"), List.of("m3"));
        c3 = clazz("c3", "t1", List.of(), List.of());

        m1 = material("m1", "c1", "t1", MaterialStatus.WAITING);
        m2 = material("m2", "c1", "t1", MaterialStatus.APPROVED);
        m3 = material("m3", "c2", "t2", MaterialStatus.WAITING);

        when(classRepository.save(any(Class.class))).thenAnswer(inv -> {
            Class saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("new-class");
            }
            return saved;
        });
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> {
            Material saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("new-material");
            }
            return saved;
        });
    }

    @AfterEach
    void verifyOpenAiInteractions() {
        // Falha se uma chamada esperada não ocorreu; chamadas inesperadas já falham no momento da requisição.
        openAi.verify();
    }

    // ---------- fixtures ----------

    private User user(String id, String email, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Usuário " + id);
        user.setRole(role);
        user.setPassword(FAKE_HASH);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private Class clazz(String id, String teacherId, List<String> studentIds, List<String> materialIds) {
        Class clazz = new Class();
        clazz.setId(id);
        clazz.setTeacherId(teacherId);
        clazz.setStudentIds(new ArrayList<>(studentIds));
        clazz.setMaterialIds(new ArrayList<>(materialIds));
        clazz.setSubject("Matemática");
        clazz.setTitle("Turma " + id);
        clazz.setDescription("Descrição " + id);
        when(classRepository.findById(id)).thenReturn(Optional.of(clazz));
        return clazz;
    }

    private Material material(String id, String classId, String teacherId, MaterialStatus status) {
        Material material = new Material();
        material.setId(id);
        material.setClassId(classId);
        material.setTeacherId(teacherId);
        material.setMaterialType(MaterialType.BASIC);
        material.setTitle("Material " + id);
        material.setDescription("Descrição " + id);
        material.setText("Conteúdo " + id);
        material.setStatus(status);
        when(materialRepository.findById(id)).thenReturn(Optional.of(material));
        return material;
    }

    private String bearer(User user) {
        AuthenticatedUser principal = AuthenticatedUser.from(user);
        return "Bearer " + jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private ResultActions postJson(String url, User as, String json) throws Exception {
        return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, bearer(as))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions patchJson(String url, User as, String json) throws Exception {
        return mvc.perform(patch(url).header(HttpHeaders.AUTHORIZATION, bearer(as))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions getAs(String url, User as) throws Exception {
        return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(as)));
    }

    private static final String NEW_CLASS_JSON = """
            {"subject":"Física","title":"Nova turma","description":"Descrição","studentIds":["s1"]}
            """;

    // ---------- turmas ----------

    @Nested
    class Classes {
        @Test
        void teacherCreatesOwnClass_teacherIdDerivedFromToken() throws Exception {
            when(userRepository.findAllById(List.of("s1"))).thenReturn(List.of(s1));

            postJson("/class/create", t1, NEW_CLASS_JSON)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("new-class"))
                    .andExpect(jsonPath("$.teacherId").value("t1"))
                    .andExpect(jsonPath("$.studentIds[0]").value("s1"))
                    .andExpect(jsonPath("$.materialIds.length()").value(0));
        }

        @Test
        void teacherCanSendOwnTeacherIdForCompatibility() throws Exception {
            when(userRepository.findAllById(List.of("s1"))).thenReturn(List.of(s1));

            postJson("/class/create", t1, """
                    {"teacherId":"t1","subject":"Física","title":"T","description":"D","studentIds":["s1"]}
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.teacherId").value("t1"));
        }

        @Test
        void teacherCannotCreateClassForAnotherTeacher_403() throws Exception {
            postJson("/class/create", t1, """
                    {"teacherId":"t2","subject":"Física","title":"T","description":"D"}
                    """)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            verify(classRepository, never()).save(any());
        }

        @Test
        void classCannotEnrollNonStudentsOrUnknownUsers_400() throws Exception {
            when(userRepository.findAllById(List.of("t2", "ghost"))).thenReturn(List.of(t2));

            postJson("/class/create", t1, """
                    {"subject":"Física","title":"T","description":"D","studentIds":["t2","ghost"]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("t2")))
                    .andExpect(jsonPath("$.error", containsString("ghost")));

            verify(classRepository, never()).save(any());
        }

        @Test
        void createClassValidatesRequiredFields_400() throws Exception {
            postJson("/class/create", t1, """
                    {"subject":"Física"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").exists());

            verify(classRepository, never()).save(any());
        }

        @Test
        void studentCannotCreateClass_403() throws Exception {
            postJson("/class/create", s1, NEW_CLASS_JSON)
                    .andExpect(status().isForbidden());

            verify(classRepository, never()).save(any());
        }

        @Test
        void teacherListsOnlyOwnClasses_withFilteredQuery() throws Exception {
            when(classRepository.findByTeacherId("t1")).thenReturn(List.of(c1, c3));

            getAs("/class/", t1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value("c1"))
                    .andExpect(jsonPath("$[1].id").value("c3"));

            verify(classRepository, never()).findAll();
        }

        @Test
        void teacherGetsOwnClassById() throws Exception {
            getAs("/class/c1", t1).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("c1"));
        }

        @Test
        void teacherCannotSeeAnotherTeachersClass_404() throws Exception {
            getAs("/class/c2", t1).andExpect(status().isNotFound());
        }

        @Test
        void studentListsOnlyEnrolledClasses() throws Exception {
            when(classRepository.findByStudentIds("s1")).thenReturn(List.of(c1));

            getAs("/class/", s1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value("c1"));

            verify(classRepository, never()).findAll();
        }

        @Test
        void studentGetsEnrolledClassById() throws Exception {
            getAs("/class/c1", s1).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("c1"));
        }

        @Test
        void studentCannotSeeOtherClass_sameResponseAsNonExistent() throws Exception {
            String foreign = getAs("/class/c2", s1)
                    .andExpect(status().isNotFound())
                    .andReturn().getResponse().getContentAsString();
            String missing = getAs("/class/does-not-exist", s1)
                    .andExpect(status().isNotFound())
                    .andReturn().getResponse().getContentAsString();

            assertThat(foreign).isEqualTo(missing);
        }

        @Test
        void updateChangesTheExistingClass_neverCreatesANewOne() throws Exception {
            patchJson("/class/update/c1", t1, """
                    {"title":"Título novo"}
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("c1"))
                    .andExpect(jsonPath("$.title").value("Título novo"))
                    .andExpect(jsonPath("$.teacherId").value("t1"))
                    .andExpect(jsonPath("$.studentIds[0]").value("s1"))
                    .andExpect(jsonPath("$.materialIds.length()").value(2));

            ArgumentCaptor<Class> saved = ArgumentCaptor.forClass(Class.class);
            verify(classRepository, times(1)).save(saved.capture());
            assertThat(saved.getValue().getId()).isEqualTo("c1");
            assertThat(saved.getValue().getSubject()).isEqualTo("Matemática");
        }

        @Test
        void teacherManagesEnrollmentOfOwnClass() throws Exception {
            when(userRepository.findAllById(List.of("s1", "s2"))).thenReturn(List.of(s1, s2));

            patchJson("/class/update/c1", t1, """
                    {"studentIds":["s1","s2","s2"]}
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentIds.length()").value(2));
        }

        @Test
        void updateCannotChangeTeacherOrMaterials_400() throws Exception {
            patchJson("/class/update/c1", t1, """
                    {"teacherId":"t2","materialIds":[]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("teacherId")))
                    .andExpect(jsonPath("$.error", containsString("materialIds")));

            verify(classRepository, never()).save(any());
            assertThat(c1.getTeacherId()).isEqualTo("t1");
        }

        @Test
        void emptyUpdateIsRejected_400() throws Exception {
            patchJson("/class/update/c1", t1, "{}").andExpect(status().isBadRequest());

            verify(classRepository, never()).save(any());
        }

        @Test
        void teacherCannotUpdateAnotherTeachersClass_404() throws Exception {
            patchJson("/class/update/c2", t1, """
                    {"title":"Invasão"}
                    """)
                    .andExpect(status().isNotFound());

            verify(classRepository, never()).save(any());
        }

        @Test
        void studentCannotUpdateClass_403() throws Exception {
            patchJson("/class/update/c1", s1, """
                    {"title":"Aluno editando"}
                    """)
                    .andExpect(status().isForbidden());

            verify(classRepository, never()).save(any());
        }

        @Test
        void teacherDeletesOwnEmptyClass() throws Exception {
            mvc.perform(delete("/class/delete/c3").header(HttpHeaders.AUTHORIZATION, bearer(t1)))
                    .andExpect(status().isOk());

            verify(classRepository).deleteById("c3");
        }

        @Test
        void classWithMaterialsIsNotDeleted_409() throws Exception {
            mvc.perform(delete("/class/delete/c1").header(HttpHeaders.AUTHORIZATION, bearer(t1)))
                    .andExpect(status().isConflict());

            verify(classRepository, never()).deleteById(anyString());
        }

        @Test
        void teacherCannotDeleteAnotherTeachersClass_404() throws Exception {
            mvc.perform(delete("/class/delete/c2").header(HttpHeaders.AUTHORIZATION, bearer(t1)))
                    .andExpect(status().isNotFound());

            verify(classRepository, never()).deleteById(anyString());
        }

        @Test
        void studentCannotDeleteClass_403() throws Exception {
            mvc.perform(delete("/class/delete/c1").header(HttpHeaders.AUTHORIZATION, bearer(s1)))
                    .andExpect(status().isForbidden());

            verify(classRepository, never()).deleteById(anyString());
        }
    }

    // ---------- materiais ----------

    @Nested
    class Materials {
        private static final String BASIC_MATERIAL_C1 = """
                {"classId":"c1","materialType":"BASIC","title":"Aula 1","description":"Resumo","text":"Conteúdo"}
                """;

        @Test
        void teacherCreatesMaterialInOwnClass_asWaiting() throws Exception {
            postJson("/material/create", t1, BASIC_MATERIAL_C1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("new-material"))
                    .andExpect(jsonPath("$.classId").value("c1"))
                    .andExpect(jsonPath("$.teacherId").value("t1"))
                    .andExpect(jsonPath("$.status").value("WAITING"));

            assertThat(c1.getMaterialIds()).contains("new-material");
        }

        @Test
        void teacherCannotCreateMaterialInAnotherTeachersClass_404() throws Exception {
            postJson("/material/create", t1, """
                    {"classId":"c2","materialType":"BASIC","title":"X","description":"Y","text":"Z"}
                    """)
                    .andExpect(status().isNotFound());

            verify(materialRepository, never()).save(any());
            assertThat(c2.getMaterialIds()).containsExactly("m3");
        }

        @Test
        void teacherCannotCreateMaterialInAnotherTeachersName_403() throws Exception {
            postJson("/material/create", t1, """
                    {"classId":"c1","teacherId":"t2","materialType":"BASIC","title":"X","description":"Y","text":"Z"}
                    """)
                    .andExpect(status().isForbidden());

            verify(materialRepository, never()).save(any());
        }

        @Test
        void studentCannotCreateMaterial_403() throws Exception {
            postJson("/material/create", s1, BASIC_MATERIAL_C1).andExpect(status().isForbidden());

            verify(materialRepository, never()).save(any());
        }

        @Test
        void basicMaterialWithoutTextIsRejected_400() throws Exception {
            postJson("/material/create", t1, """
                    {"classId":"c1","materialType":"BASIC","title":"X","description":"Y"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Text cannot be empty in BASIC material"));

            verify(materialRepository, never()).save(any());
        }

        @Test
        void teacherApprovesOwnWaitingMaterial() throws Exception {
            postJson("/material/status/m1/APPROVED", t1, "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(materialRepository).save(m1);
        }

        @Test
        void teacherCannotApproveAnotherTeachersMaterial_404() throws Exception {
            postJson("/material/status/m3/APPROVED", t1, "").andExpect(status().isNotFound());

            verify(materialRepository, never()).save(any());
            assertThat(m3.getStatus()).isEqualTo(MaterialStatus.WAITING);
        }

        @Test
        void studentCannotApproveMaterial_403() throws Exception {
            postJson("/material/status/m1/APPROVED", s1, "").andExpect(status().isForbidden());

            verify(materialRepository, never()).save(any());
            assertThat(m1.getStatus()).isEqualTo(MaterialStatus.WAITING);
        }

        @Test
        void invalidTransitionsAreRejected_409() throws Exception {
            postJson("/material/status/m2/APPROVED", t1, "").andExpect(status().isConflict());   // APPROVED -> APPROVED
            postJson("/material/status/m2/WAITING", t1, "").andExpect(status().isConflict());    // APPROVED -> WAITING
            postJson("/material/status/m1/WAITING", t1, "").andExpect(status().isConflict());    // WAITING -> WAITING

            verify(materialRepository, never()).save(any());
        }

        @Test
        void unknownStatusValueIsRejected_400() throws Exception {
            postJson("/material/status/m1/PUBLISHED", t1, "").andExpect(status().isBadRequest());

            verify(materialRepository, never()).save(any());
        }

        @Test
        void studentListsOnlyApprovedMaterialsOfEnrolledClass() throws Exception {
            when(materialRepository.findByClassIdAndStatus("c1", MaterialStatus.APPROVED)).thenReturn(List.of(m2));

            getAs("/material/c1", s1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value("m2"))
                    .andExpect(jsonPath("$[0].status").value("APPROVED"));

            // Materiais pendentes nem chegam a ser consultados para o aluno.
            verify(materialRepository, never()).findByClassId(anyString());
        }

        @Test
        void studentCannotListMaterialsOfAnotherClass_404() throws Exception {
            getAs("/material/c2", s1).andExpect(status().isNotFound());

            verify(materialRepository, never()).findByClassId(anyString());
            verify(materialRepository, never()).findByClassIdAndStatus(anyString(), any());
        }

        @Test
        void teacherListsAllMaterialsOfOwnClass() throws Exception {
            when(materialRepository.findByClassId("c1")).thenReturn(List.of(m1, m2));

            getAs("/material/c1", t1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void teacherCannotListMaterialsOfAnotherTeachersClass_404() throws Exception {
            getAs("/material/c1", t2).andExpect(status().isNotFound());

            verify(materialRepository, never()).findByClassId(anyString());
        }
    }

    // ---------- IA ----------

    @Nested
    class ArtificialIntelligence {
        private static final String LESSON_TEXT = "Hoje estudamos frações e números decimais.";

        private String summarizeJson(String classId) {
            return "{\"text\":\"" + LESSON_TEXT + "\",\"classId\":\"" + classId + "\"}";
        }

        @Test
        void teacherSummarizesOwnClass_sendsOnlyTextAndCreatesWaitingMaterial() throws Exception {
            openAi.expect(MockRestRequestMatchers.requestTo(CHAT_URL))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                    .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer test-only-key-not-a-real-credential"))
                    .andExpect(MockRestRequestMatchers.jsonPath("$.model").value("gpt-4o-mini"))
                    .andExpect(MockRestRequestMatchers.jsonPath("$.messages[1].role").value("user"))
                    .andExpect(MockRestRequestMatchers.jsonPath("$.messages[1].content").value(LESSON_TEXT))
                    .andExpect(MockRestRequestMatchers.content().string(not(containsString("SummarizeRequestDTO"))))
                    .andExpect(MockRestRequestMatchers.content().string(not(containsString("classId"))))
                    .andRespond(MockRestResponseCreators.withSuccess(
                            "{\"choices\":[{\"message\":{\"content\":\"Resumo gerado\"}}]}", MediaType.APPLICATION_JSON));

            postJson("/ai/summarize", t1, summarizeJson("c1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Resumo gerado"));

            ArgumentCaptor<Material> saved = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(saved.capture());
            assertThat(saved.getValue().getClassId()).isEqualTo("c1");
            assertThat(saved.getValue().getTeacherId()).isEqualTo("t1");
            assertThat(saved.getValue().getText()).isEqualTo("Resumo gerado");
            assertThat(saved.getValue().getStatus()).isEqualTo(MaterialStatus.WAITING);
        }

        @Test
        void studentCannotSummarize_403_andOpenAiIsNotCalled() throws Exception {
            postJson("/ai/summarize", s1, summarizeJson("c1")).andExpect(status().isForbidden());

            verify(materialRepository, never()).save(any());
        }

        @Test
        void teacherCannotSummarizeIntoAnotherTeachersClass_404_andOpenAiIsNotCalled() throws Exception {
            postJson("/ai/summarize", t1, summarizeJson("c2")).andExpect(status().isNotFound());

            verify(materialRepository, never()).save(any());
        }

        @Test
        void teacherCannotSummarizeInAnotherTeachersName_403_andOpenAiIsNotCalled() throws Exception {
            postJson("/ai/summarize", t1,
                    "{\"text\":\"" + LESSON_TEXT + "\",\"classId\":\"c1\",\"teacherId\":\"t2\"}")
                    .andExpect(status().isForbidden());

            verify(materialRepository, never()).save(any());
        }

        @Test
        void openAiFailureReturns502WithoutDetailsAndCreatesNoMaterial() throws Exception {
            openAi.expect(MockRestRequestMatchers.requestTo(CHAT_URL))
                    .andRespond(MockRestResponseCreators.withServerError()
                            .body("{\"error\":\"internal details\"}").contentType(MediaType.APPLICATION_JSON));

            String body = postJson("/ai/summarize", t1, summarizeJson("c1"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error").value("Failed to communicate with the AI service."))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("internal details").doesNotContain("test-only-key");
            verify(materialRepository, never()).save(any());
        }

        @Test
        void teacherTranscribesAudio() throws Exception {
            openAi.expect(MockRestRequestMatchers.requestTo(TRANSCRIBE_URL))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                    .andRespond(MockRestResponseCreators.withSuccess(
                            "{\"text\":\"transcrição da aula\",\"usage\":null}", MediaType.APPLICATION_JSON));

            mvc.perform(multipart("/ai/transcribe")
                            .file(new MockMultipartFile("file", "aula.mp3", "audio/mpeg", new byte[]{1, 2, 3}))
                            .header(HttpHeaders.AUTHORIZATION, bearer(t1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("transcrição da aula"));
        }

        @Test
        void studentCannotTranscribe_403_andOpenAiIsNotCalled() throws Exception {
            mvc.perform(multipart("/ai/transcribe")
                            .file(new MockMultipartFile("file", "aula.mp3", "audio/mpeg", new byte[]{1, 2, 3}))
                            .header(HttpHeaders.AUTHORIZATION, bearer(s1)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void emptyAudioIsRejected_400_andOpenAiIsNotCalled() throws Exception {
            mvc.perform(multipart("/ai/transcribe")
                            .file(new MockMultipartFile("file", "vazio.mp3", "audio/mpeg", new byte[0]))
                            .header(HttpHeaders.AUTHORIZATION, bearer(t1)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------- regressão ----------

    @Nested
    class Regression {
        @Test
        void requestsWithoutTokenReturn401() throws Exception {
            mvc.perform(get("/class/")).andExpect(status().isUnauthorized());
            mvc.perform(get("/material/c1")).andExpect(status().isUnauthorized());
            mvc.perform(post("/ai/summarize").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void invalidJwtReturns401() throws Exception {
            mvc.perform(get("/class/c1").header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(SecurityErrorResponses.UNAUTHENTICATED_MESSAGE));
        }

        @Test
        void userWithoutRoleCannotUseTeacherOrStudentFeatures() throws Exception {
            User legacy = user("l1", "legado@test.com", null);

            getAs("/class/", legacy).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            getAs("/class/c1", legacy).andExpect(status().isNotFound());
            postJson("/class/create", legacy, NEW_CLASS_JSON).andExpect(status().isForbidden());
        }
    }
}
