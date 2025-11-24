package com.sensei.app.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "turmas")
public class turma {

    @Id
    private String id;

    private String nome;

    private String codigo;

    private String professorId;

    private List<String> alunosIds;

    private List<String> conteudosIds;
}
