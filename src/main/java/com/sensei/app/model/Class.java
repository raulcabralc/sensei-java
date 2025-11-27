package com.sensei.app.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "classes")
public class Class {
    @Id
    private String id;

    private String teacherId;

    private List<String> studentIds;

    private List<String> materialIds;

    private String subject;

    private String title;

    private String description;
}
