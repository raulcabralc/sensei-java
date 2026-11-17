package com.sensei.app.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sensei.app.model.Class;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representação de turma na API.
 * - Professor responsável: todos os campos, incluindo matrículas (studentIds) e materialIds.
 * - Aluno matriculado: somente dados descritivos; studentIds e materialIds são omitidos
 *   (não expõe colegas nem referências a materiais pendentes).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassResponseDTO(
        String id,
        String teacherId,
        List<String> studentIds,
        List<String> materialIds,
        String subject,
        String title,
        String description
) {
    public static ClassResponseDTO forTeacher(Class clazz) {
        return new ClassResponseDTO(
                clazz.getId(),
                clazz.getTeacherId(),
                copyOf(clazz.getStudentIds()),
                copyOf(clazz.getMaterialIds()),
                clazz.getSubject(),
                clazz.getTitle(),
                clazz.getDescription()
        );
    }

    public static ClassResponseDTO forStudent(Class clazz) {
        return new ClassResponseDTO(
                clazz.getId(),
                clazz.getTeacherId(),
                null,
                null,
                clazz.getSubject(),
                clazz.getTitle(),
                clazz.getDescription()
        );
    }

    private static List<String> copyOf(List<String> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
