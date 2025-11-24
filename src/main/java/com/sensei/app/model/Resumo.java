package com.sensei.app.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "resumos")
public class Resumo {

    @Id
    private String id;

    private String turmaId;

    private String conteudoId;

    private String texto;

    private ConteudoStatus status;

    private String professorResponsavelId;
}
