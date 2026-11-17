package com.sensei.app.config;

import com.sensei.app.dtos.CreateMaterialDTO;
import com.sensei.app.dtos.SummarizeRequestDTO;
import com.sensei.app.enums.UserRole;
import com.sensei.app.exceptions.ExternalServiceException;
import com.sensei.app.model.Class;
import com.sensei.app.security.AuthenticatedUser;
import com.sensei.app.service.ClassService;
import com.sensei.app.service.MaterialService;
import com.sensei.app.service.OpenAiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Timeouts reais dos clientes HTTP da OpenAI, sem rede externa: um servidor local em loopback
 * aceita a conexão (backlog do sistema operacional) e nunca responde.
 */
class OpenAiTimeoutTest {
    private static final Duration SHORT_READ = Duration.ofMillis(300);
    private static final Duration MAX_EXPECTED = Duration.ofSeconds(5);

    private ServerSocket silentServer;
    private String baseUrl;

    private final MaterialService materialService = mock(MaterialService.class);
    private final ClassService classService = mock(ClassService.class);
    private final AuthenticatedUser teacher = new AuthenticatedUser("t1", "prof@test.com", null, UserRole.TEACHER);

    @BeforeEach
    void startSilentServer() throws IOException {
        silentServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        baseUrl = "http://127.0.0.1:" + silentServer.getLocalPort();

        Class ownedClass = new Class();
        ownedClass.setId("c1");
        ownedClass.setTeacherId("t1");
        when(classService.getOwnedClass("c1", teacher)).thenReturn(ownedClass);
    }

    @AfterEach
    void stopSilentServer() throws IOException {
        silentServer.close();
    }

    private OpenAiService service(RestTemplate chat, RestTemplate transcription, String chatUrl, String transcriptionUrl) {
        return new OpenAiService(materialService, classService, chat, transcription,
                "test-only-key-not-a-real-credential", transcriptionUrl, chatUrl);
    }

    private static SummarizeRequestDTO summary() {
        return new SummarizeRequestDTO("Aula sobre frações", "c1", null);
    }

    @Test
    void summaryReadTimeoutBecomesExternalServiceErrorAndCreatesNoMaterial() {
        RestTemplate chat = OpenAiClientConfig.restTemplate(Duration.ofSeconds(2), SHORT_READ);
        OpenAiService service = service(chat, chat, baseUrl + "/v1/chat/completions", baseUrl + "/unused");

        long start = System.nanoTime();
        assertThatThrownBy(() -> service.summarizeText(summary(), teacher))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Failed to communicate with the AI service.")
                .hasCauseInstanceOf(ResourceAccessException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(MAX_EXPECTED);

        verify(materialService, never()).createMaterial(any(CreateMaterialDTO.class), any());
    }

    @Test
    void transcriptionReadTimeoutBecomesExternalServiceError() {
        RestTemplate transcription = OpenAiClientConfig.restTemplate(Duration.ofSeconds(2), SHORT_READ);
        OpenAiService service = service(transcription, transcription, baseUrl + "/unused", baseUrl + "/v1/audio/transcriptions");

        assertThatThrownBy(() -> service.transcribeAudio(
                new MockMultipartFile("file", "aula.mp3", "audio/mpeg", new byte[]{1, 2, 3}), teacher))
                .isInstanceOf(ExternalServiceException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void refusedConnectionBecomesExternalServiceError() throws IOException {
        int closedPort;
        try (ServerSocket temporary = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = temporary.getLocalPort();
        }
        RestTemplate chat = OpenAiClientConfig.restTemplate(Duration.ofSeconds(2), SHORT_READ);
        OpenAiService service = service(chat, chat, "http://127.0.0.1:" + closedPort + "/v1/chat/completions", "unused");

        assertThatThrownBy(() -> service.summarizeText(summary(), teacher))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Failed to communicate with the AI service.");

        verify(materialService, never()).createMaterial(any(CreateMaterialDTO.class), any());
    }

    // ---------- configuração ----------

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OpenAiClientConfig.class);

    @Test
    void defaultTimeoutsGiveTranscriptionMoreTimeThanSummary() {
        contextRunner.run(context -> {
            OpenAiTimeoutProperties timeouts = context.getBean(OpenAiTimeoutProperties.class);

            assertThat(timeouts.connect()).isEqualTo(Duration.ofSeconds(10));
            assertThat(timeouts.chatRead()).isEqualTo(Duration.ofSeconds(90));
            assertThat(timeouts.transcriptionRead()).isEqualTo(Duration.ofSeconds(300));
            assertThat(timeouts.transcriptionRead()).isGreaterThan(timeouts.chatRead());
            assertThat(context).hasBean("openAiChatRestTemplate").hasBean("openAiTranscriptionRestTemplate");
        });
    }

    @Test
    void configuredReadTimeoutIsAppliedToTheChatClient() {
        contextRunner
                .withPropertyValues("openai.timeout.chat-read=300ms", "openai.timeout.transcription-read=5s")
                .run(context -> {
                    RestTemplate chat = context.getBean("openAiChatRestTemplate", RestTemplate.class);

                    long start = System.nanoTime();
                    assertThatThrownBy(() -> chat.postForEntity(baseUrl + "/v1/chat/completions", "{}", String.class))
                            .isInstanceOf(ResourceAccessException.class)
                            .hasRootCauseInstanceOf(SocketTimeoutException.class);
                    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(MAX_EXPECTED);
                });
    }

    @Test
    void nonPositiveTimeoutPreventsStartup() {
        contextRunner
                .withPropertyValues("openai.timeout.chat-read=0s")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("openai.timeout.chat-read must be greater than zero"));
    }
}
