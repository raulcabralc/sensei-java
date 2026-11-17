package com.sensei.app.dtos;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Campos atualizáveis de uma turma: subject, title, description e studentIds (matrículas).
 * Campos ausentes (null) não são alterados. teacherId, materialIds, id e qualquer outro
 * campo são registrados em {@link #getUnsupportedFields()} e a atualização é rejeitada.
 */
@Data
@NoArgsConstructor
public class UpdateClassDTO {
    private List<String> studentIds;

    private String subject;

    private String title;

    private String description;

    @JsonIgnore
    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonAnySetter
    public void registerUnsupportedField(String field, Object ignoredValue) {
        this.unsupportedFields.add(field);
    }

    public Set<String> getUnsupportedFields() {
        return Collections.unmodifiableSet(unsupportedFields);
    }
}
