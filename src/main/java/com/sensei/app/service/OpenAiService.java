package com.sensei.app.service;

import com.sensei.app.dtos.OpenAiRequestDTO;
import com.sensei.app.dtos.OpenAiResponseDTO;
import com.sensei.app.dtos.ResponseTranscribeDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class OpenAiService {
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.chat-url}")
    private String chatUrl;

    public ResponseTranscribeDTO transcribeAudio(MultipartFile file) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource fileSource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        body.add("file", fileSource);
        body.add("model", "whisper-1");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ResponseTranscribeDTO> response = restTemplate.postForEntity(apiUrl, requestEntity, ResponseTranscribeDTO.class);

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error calling OpenAI API: ", e.getCause());
        }
    }

    public String summarizeText(String text) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String systemPrompt = "Você é um assistente especialista em resumir textos acadêmicos e técnicos. " +
                "Seu objetivo é criar um resumo conciso, em tópicos (bullet points), " +
                "destacando as ideias principais. O idioma deve ser Português. " +
                "Lembre-se de simplificar o texto, para que seja melhor para a copreensão.";

        var messages = List.of(
                new OpenAiRequestDTO.Message("system", systemPrompt),
                new OpenAiRequestDTO.Message("user", text)
        );

        var requestBody = new OpenAiRequestDTO("gpt-4o-mini", messages);
        HttpEntity<OpenAiRequestDTO> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<OpenAiResponseDTO> response = restTemplate.postForEntity(
                    chatUrl,
                    requestEntity,
                    OpenAiResponseDTO.class
            );

            if (response.getBody() != null && !response.getBody().choices().isEmpty()) {
                return response.getBody().choices().get(0).message().content();
            }

            return "Unable to summarize text. ";
        } catch (Exception e) {
            throw new RuntimeException("Error calling OpenAI API: ", e.getCause());
        }
    }
}
