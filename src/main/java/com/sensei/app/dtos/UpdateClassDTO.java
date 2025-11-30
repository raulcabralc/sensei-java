package com.sensei.app.dtos;

import com.sensei.app.model.Class;
import lombok.Data;

import java.util.List;

@Data
public class UpdateClassDTO {
    private String teacherId;

    private List<String> studentIds;

    private List<String> materialIds;

    private String subject;

    private String title;

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
