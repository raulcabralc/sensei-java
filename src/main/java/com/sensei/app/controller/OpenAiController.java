package com.sensei.app.controller;

import com.sensei.app.dtos.ResponseTranscribeDTO;
import com.sensei.app.dtos.SummarizeRequestDTO;
import com.sensei.app.security.AuthenticatedUser;
import com.sensei.app.service.OpenAiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Operações que consomem a API da OpenAI: restritas a professores.
 */
@RestController
@RequestMapping("/ai")
@PreAuthorize("hasRole('TEACHER')")
public class OpenAiController {
    private final OpenAiService openAiService;

    public OpenAiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<ResponseTranscribeDTO> transcribe(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(this.openAiService.transcribeAudio(file, currentUser));
    }

    @PostMapping("/summarize")
    public ResponseEntity<String> summarize(
            @Valid @RequestBody SummarizeRequestDTO summarizeDTO,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        // Somente o campo de texto é enviado à OpenAI (não a representação completa do DTO).
        return ResponseEntity.ok(this.openAiService.summarizeText(summarizeDTO, currentUser));
    }
}
