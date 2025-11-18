package com.sensei.app.model;

import com.sensei.app.enums.UserRole;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "teachers")
@Data
public class User {
    @Id
    private String id;

    private String name;

    private String email;

    private UserRole role;

    private String password;
}
