package com.sensei.app.dtos;

import com.sensei.app.enums.MaterialType;
import com.sensei.app.model.Material;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateMaterialDTO {
    @NotNull(message = "ClassId is required.")
    private String classId;

    @NotNull(message = "TeacherId is required.")
    private String teacherId;

    @NotNull(message = "MaterialType is required.")
    private MaterialType materialType;

    @NotNull(message = "Title is required.")
    private String title;

    @NotNull(message = "Description is required.")
    private String description;

    private String text;

    private List<TaskDTO> tasks;

    public Material toEntity() {
        Material material = new Material();

        material.setClassId(this.classId);
        material.setTeacherId(this.teacherId);
        material.setMaterialType(this.materialType);
        material.setTitle(this.title);
        material.setDescription(this.description);
        material.setText(this.text);
        material.setTasks(this.tasks);

        return material;
    }
}
