package com.sensei.app.dtos;

import com.sensei.app.model.Class;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateClassDTO {
    @NotNull
    private String teacherId;

    private List<String> studentIds;

    @NotNull
    private String subject;

    @NotNull
    private String title;

    @NotNull
    private String description;

    public Class toEntity() {
        Class newClass = new Class();

        newClass.setTeacherId(teacherId);
        newClass.setStudentIds(studentIds);
        newClass.setSubject(subject);
        newClass.setTitle(title);
        newClass.setDescription(description);

        return newClass;
    }
}
