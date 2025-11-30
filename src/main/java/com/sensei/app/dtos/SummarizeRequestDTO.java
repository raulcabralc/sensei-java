package com.sensei.app.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummarizeRequestDTO {
    @NotBlank(message = "Text was not provided. Use 'text' to type the text.")
    private String text;

    @NotBlank(message = "ClassId was not provided. Use 'classId' to type the text.")
    private String classId;

    @NotBlank(message = "TeacherId was not provided. Use 'teacherId' to type the text.")
    private String teacherId;
}