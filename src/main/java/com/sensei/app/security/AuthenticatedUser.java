package com.sensei.app.security;

import com.sensei.app.enums.UserRole;
import com.sensei.app.model.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal autenticado da aplicação.
 * Mantém o ID persistido do usuário para que as verificações de propriedade
 * não dependam de identificadores enviados pelo cliente.
 * O username continua sendo o e-mail (subject do JWT).
 */
public class AuthenticatedUser implements UserDetails, CredentialsContainer {
    public static final String ROLE_PREFIX = "ROLE_";

    private final String id;
    private final String email;
    private final UserRole role;
    private final List<GrantedAuthority> authorities;
    private String password;

    public AuthenticatedUser(String id, String email, String password, UserRole role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.authorities = authoritiesFor(role);
    }

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }

    /**
     * TEACHER -> ROLE_TEACHER, STUDENT -> ROLE_STUDENT.
     * Documentos sem papel definido não recebem nenhuma authority.
     */
    public static List<GrantedAuthority> authoritiesFor(UserRole role) {
        if (role == null) {
            return List.of();
        }

        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }

    public String getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
