package com.sensei.app.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "materiais_estudo")
public class MaterialEstudo {

    @Id
    private String id;

    private String turmaId;

    private String conteudoId;

    private String titulo;

    private String textoGerado;

    private ConteudoStatus status;

    private String professorResponsavelId;
}
