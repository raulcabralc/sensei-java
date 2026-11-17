package com.sensei.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sensei.app.enums.UserRole;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "users")
@Data
public class User {
    @Id
    private String id;

    private String name;

    private String email;

    private UserRole role;

    // Proteção defensiva: o hash nunca é serializado em JSON, mesmo que a entidade
    // seja retornada por engano. Não afeta o mapeamento do MongoDB.
    @JsonIgnore
    private String password;

    private List<String> classIds;
}
