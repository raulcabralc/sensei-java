package com.sensei.app.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "conteudos")
public class conteudo {

    @Id
    private String id;

    private String turmaId;

    private String titulo;

    private String texto;

    private ConteudoStatus status;

    private String professorResponsavelId;
}
