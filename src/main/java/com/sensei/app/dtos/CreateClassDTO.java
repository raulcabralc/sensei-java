package com.sensei.app.dtos;

import com.sensei.app.model.Class;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateClassDTO {
    /**
     * Opcional. O professor responsável é sempre o usuário autenticado; se informado,
     * deve ser igual ao ID dele (mantido por compatibilidade com clientes existentes).
     */
    private String teacherId;

    private List<String> studentIds;

    @NotNull
    private String subject;

    @NotNull
    private String title;

    @NotNull
    private String description;

    public Class toEntity(String ownerTeacherId, List<String> validatedStudentIds) {
        Class newClass = new Class();

        newClass.setTeacherId(ownerTeacherId);
        newClass.setStudentIds(new ArrayList<>(validatedStudentIds));
        newClass.setMaterialIds(new ArrayList<>());
        newClass.setSubject(subject);
        newClass.setTitle(title);
        newClass.setDescription(description);

        return newClass;
    }
}
