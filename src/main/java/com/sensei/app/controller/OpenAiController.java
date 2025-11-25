package com.sensei.app.controller;

import com.sensei.app.dtos.ResponseTranscribeDTO;
import com.sensei.app.service.OpenAiService;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
}
