package com.sensei.app.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TaskDTO {
    private String taskTitle;

    private String taskDescription;

    private List<TaskOptionDTO> taskOptions;

    private int correctOption;
}
