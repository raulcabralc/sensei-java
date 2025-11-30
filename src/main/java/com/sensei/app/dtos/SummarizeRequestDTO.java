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
}