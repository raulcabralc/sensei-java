package com.sensei.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensei.app.enums.UserRole;
import com.sensei.app.model.User;
import com.sensei.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository);

    private static User user(String id, String email, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Nome");
        user.setRole(role);
        user.setPassword("$2a$10$hashedPasswordValueForTestsOnly000000000000000000000");
        return user;
    }

    @Test
    void teacherReceivesRoleTeacher() {
        when(userRepository.findByEmail("prof@test.com")).thenReturn(Optional.of(user("t1", "prof@test.com", UserRole.TEACHER)));

        UserDetails details = service.loadUserByUsername("prof@test.com");

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_TEACHER");
        assertThat(details).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) details).getId()).isEqualTo("t1");
        assertThat(details.getUsername()).isEqualTo("prof@test.com");
    }

    @Test
    void studentReceivesRoleStudent() {
        when(userRepository.findByEmail("aluno@test.com")).thenReturn(Optional.of(user("s1", "aluno@test.com", UserRole.STUDENT)));

        UserDetails details = service.loadUserByUsername("aluno@test.com");

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_STUDENT");
    }

    @Test
    void userWithoutRoleReceivesNoAuthorities() {
        when(userRepository.findByEmail("legado@test.com")).thenReturn(Optional.of(user("l1", "legado@test.com", null)));

        assertThat(service.loadUserByUsername("legado@test.com").getAuthorities()).isEmpty();
    }

    @Test
    void unknownUserThrowsWithoutEchoingEmail() {
        when(userRepository.findByEmail("x@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("x@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageNotContaining("x@test.com");
    }

    @Test
    void userEntityNeverSerializesPassword() throws Exception {
        String json = new ObjectMapper().writeValueAsString(user("s1", "aluno@test.com", UserRole.STUDENT));

        assertThat(json).doesNotContain("password").doesNotContain("$2a$");
    }
}
