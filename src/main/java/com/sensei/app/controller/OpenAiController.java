package com.sensei.app.controller;

import com.sensei.app.dtos.ResponseTranscribeDTO;
import com.sensei.app.dtos.SummarizeRequestDTO;
import com.sensei.app.service.OpenAiService;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai")
public class OpenAiController {
    private final OpenAiService openAiService;

    public OpenAiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<ResponseTranscribeDTO> transcribe(@RequestParam("file") MultipartFile file) throws BadRequestException {
        try {
            if (file.isEmpty()) {
                throw new BadRequestException("Failed to transcribe file");
            }

            ResponseTranscribeDTO transcription = this.openAiService.transcribeAudio(file);

            return ResponseEntity.ok(transcription);
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @PostMapping("/summarize")
    public ResponseEntity<String> summarize(@Valid @RequestBody SummarizeRequestDTO text) throws BadRequestException {
        try {
            if (text.getText().isEmpty()) {
                throw new BadRequestException("No text provided");
            }

            String stringText = text.toString();

            if (stringText == null || stringText.isEmpty()) {
                throw new BadRequestException("No text provided");
            }

            String summarizedText = this.openAiService.summarizeText(stringText);

            return ResponseEntity.ok(summarizedText);
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }
}