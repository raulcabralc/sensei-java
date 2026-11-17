package com.sensei.app.service;

import com.sensei.app.dtos.CreateMaterialDTO;
import com.sensei.app.dtos.OpenAiRequestDTO;
import com.sensei.app.dtos.OpenAiResponseDTO;
import com.sensei.app.dtos.ResponseTranscribeDTO;
import com.sensei.app.dtos.SummarizeRequestDTO;
import com.sensei.app.enums.MaterialType;
import com.sensei.app.exceptions.ExternalServiceException;
import com.sensei.app.exceptions.InvalidRequestException;
import com.sensei.app.model.Class;
import com.sensei.app.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Operações de IA (consomem créditos da OpenAI): restritas a professores.
 * Toda autorização e validação acontece ANTES de qualquer chamada externa.
 */
@Service
public class OpenAiService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private static final String AI_SERVICE_ERROR = "Failed to communicate with the AI service.";

    private final String apiKey;
    private final String apiUrl;
    private final String chatUrl;

    private final MaterialService materialService;
    private final ClassService classService;
    private final RestTemplate chatRestTemplate;
    private final RestTemplate transcriptionRestTemplate;

    public OpenAiService(
            MaterialService materialService,
            ClassService classService,
            @Qualifier("openAiChatRestTemplate") RestTemplate chatRestTemplate,
            @Qualifier("openAiTranscriptionRestTemplate") RestTemplate transcriptionRestTemplate,
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.url}") String apiUrl,
            @Value("${openai.api.chat-url}") String chatUrl
    ) {
        this.materialService = materialService;
        this.classService = classService;
        this.chatRestTemplate = chatRestTemplate;
        this.transcriptionRestTemplate = transcriptionRestTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.chatUrl = chatUrl;
    }

    /**
     * Transcrição avulsa: não está vinculada a turma nem cria material.
     * Política: somente professores (o fluxo do produto é o professor gravando a aula).
     */
    public ResponseTranscribeDTO transcribeAudio(MultipartFile file, AuthenticatedUser user) {
        AccessChecks.requireTeacher(user);

        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("An audio file is required.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource fileSource;
        try {
            fileSource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (IOException e) {
            throw new InvalidRequestException("The audio file could not be read.");
        }

        body.add("file", fileSource);
        body.add("model", "whisper-1");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ResponseTranscribeDTO> response = transcriptionRestTemplate.postForEntity(apiUrl, requestEntity, ResponseTranscribeDTO.class);

            return response.getBody();
        } catch (RestClientException e) {
            throw externalFailure("transcription", e);
        }
    }

    /**
     * Resume o texto e cria um material BASIC (WAITING) na turma informada.
     * A turma precisa pertencer ao professor autenticado; isso é verificado antes da chamada à OpenAI.
     */
    public String summarizeText(SummarizeRequestDTO summarizeDTO, AuthenticatedUser user) {
        AccessChecks.requireTeacher(user);
        AccessChecks.assertDeclaredTeacherMatches(summarizeDTO.getTeacherId(), user);

        Class clazz = this.classService.getOwnedClass(summarizeDTO.getClassId(), user);

        String text = summarizeDTO.getText();

        if (!StringUtils.hasText(text)) {
            throw new InvalidRequestException("No text provided");
        }

        String content = this.requestSummary(text);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateFormat = LocalDate.now().format(formatter);

        CreateMaterialDTO newSummarizedMaterial = new CreateMaterialDTO();

        newSummarizedMaterial.setClassId(clazz.getId());
        newSummarizedMaterial.setMaterialType(MaterialType.BASIC);
        newSummarizedMaterial.setTitle("Aula - " + dateFormat);
        newSummarizedMaterial.setDescription("Resumo da aula do dia " + dateFormat);
        newSummarizedMaterial.setText(content);

        this.materialService.createMaterial(newSummarizedMaterial, user);

        return content;
    }

    private String requestSummary(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String systemPrompt = "Você é um assistente especialista em resumir textos acadêmicos e técnicos. " +
                "Seu objetivo é criar um resumo conciso', " +
                "destacando as ideias principais. O idioma deve ser Português. " +
                "Lembre-se de simplificar o texto, para que seja melhor para a copreensão.";

        var messages = List.of(
                new OpenAiRequestDTO.Message("system", systemPrompt),
                new OpenAiRequestDTO.Message("user", text)
        );

        var requestBody = new OpenAiRequestDTO("gpt-4o-mini", messages);
        HttpEntity<OpenAiRequestDTO> requestEntity = new HttpEntity<>(requestBody, headers);

        OpenAiResponseDTO responseBody;
        try {
            responseBody = chatRestTemplate.postForEntity(chatUrl, requestEntity, OpenAiResponseDTO.class).getBody();
        } catch (RestClientException e) {
            throw externalFailure("summary", e);
        }

        if (responseBody == null || responseBody.choices() == null || responseBody.choices().isEmpty()
                || responseBody.choices().get(0).message() == null
                || !StringUtils.hasText(responseBody.choices().get(0).message().content())) {
            throw new ExternalServiceException("The AI service returned no summary.");
        }

        return responseBody.choices().get(0).message().content();
    }

    /**
     * Registra somente o tipo de erro e o status HTTP (nunca a chave nem o corpo da resposta).
     */
    private static ExternalServiceException externalFailure(String operation, RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            log.warn("OpenAI {} request failed with HTTP {}", operation, responseException.getStatusCode().value());
        } else if (e.getCause() instanceof SocketTimeoutException) {
            log.warn("OpenAI {} request timed out", operation);
        } else {
            // Falha de conexão (recusada, DNS, TLS...) ou outro erro de I/O.
            log.warn("OpenAI {} request failed: {}", operation,
                    e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName());
        }

        return new ExternalServiceException(AI_SERVICE_ERROR, e);
    }
}
