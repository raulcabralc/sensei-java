package com.sensei.app.model;

import com.sensei.app.dtos.TaskDTO;
import com.sensei.app.enums.MaterialStatus;
import com.sensei.app.enums.MaterialType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "materials")
public class Material {
    @Id
    private String id;

    private String classId;

    private String teacherId;

    private MaterialType materialType;

    private String title;

    private String description;

    private String text;

    private List<TaskDTO> tasks;

    private MaterialStatus status;
}
