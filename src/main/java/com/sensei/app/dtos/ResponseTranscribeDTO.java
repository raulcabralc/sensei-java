package com.sensei.app.dtos;

import lombok.Data;

@Data
public class ResponseTranscribeDTO {
    private final String text;
    private final Object usage;

    public ResponseTranscribeDTO(String text, Object usage) {
        this.text = text;
        this.usage = usage;
    }
}
