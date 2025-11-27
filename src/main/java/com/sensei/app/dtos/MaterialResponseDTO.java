package com.sensei.app.dtos;

import com.sensei.app.enums.MaterialStatus;
import com.sensei.app.enums.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MaterialResponseDTO {
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
