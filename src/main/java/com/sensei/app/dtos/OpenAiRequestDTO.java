package com.sensei.app.dtos;

import java.util.List;

public record OpenAiRequestDTO(String model, List<Message> messages) {
    public record Message(String role, String content) {}
}