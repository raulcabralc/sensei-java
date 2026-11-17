package com.sensei.app.dtos;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Campos atualizáveis da própria conta: somente name e email.
 * Qualquer outro campo enviado (role, id, password, ...) é registrado em
 * {@link #getUnsupportedFields()} e a atualização é rejeitada pelo UserService.
 */
@Data
@NoArgsConstructor
public class UpdateUserDTO {
    private String name;

    @Email(message = "Invalid email.")
    private String email;

    @JsonIgnore
    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    public UpdateUserDTO(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @JsonAnySetter
    public void registerUnsupportedField(String field, Object ignoredValue) {
        this.unsupportedFields.add(field);
    }

    public Set<String> getUnsupportedFields() {
        return Collections.unmodifiableSet(unsupportedFields);
    }
}
