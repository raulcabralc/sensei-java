package com.sensei.app.dtos;

import java.util.List;

public record OpenAiResponseDTO(List<Choice> choices) {
    public record Choice(Message message) {}

    public record Message(String content) {}
}