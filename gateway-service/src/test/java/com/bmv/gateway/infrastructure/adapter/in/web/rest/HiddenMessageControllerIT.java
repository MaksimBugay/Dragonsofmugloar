package com.bmv.gateway.infrastructure.adapter.in.web.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import com.bmv.dragonsofmugloar.application.commandment.request.BuildHiddenMessageRequest;
import com.bmv.dragonsofmugloar.application.commandment.response.HiddenMessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Calls the real OpenAI-compatible endpoint. OPENAI_URL_ENV is the full Responses API URL
 * and OPENAI_API_KEY_ENV is its access token. Skipped unless both are set.
 */
@SpringBootTest(properties = "spring.test.webtestclient.timeout=PT2M")
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "OPENAI_URL_ENV", matches = ".*\\S.*")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY_ENV", matches = ".*\\S.*")
class HiddenMessageControllerIT {

    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void openAiProperties(DynamicPropertyRegistry registry) {
        registry.add("bmv.ai.port.open-ai.url", () -> System.getenv("OPENAI_URL_ENV"));
        registry.add("bmv.ai.port.open-ai.access-key", () -> System.getenv("OPENAI_API_KEY_ENV"));
    }

    @Test
    void generatesHiddenMessage() {
        var request = new BuildHiddenMessageRequest(
                "Планы на выходные",
                "Дружелюбный и неформальный",
                List.of("Привет! Что будешь делать в субботу?"),
                List.of("Как дела?")
        );

        var response = generateHiddenMessage(request);

        assertFalse(request.prohibitedMessages().contains(response.text()));
    }

    @Test
    void buildsTenHiddenMessagesWithAccumulatedHistoryThroughTheRealOpenAiEndpoint() {
        var history = new ArrayList<String>();

        for (int i = 0; i < 10; i++) {
            var request = new BuildHiddenMessageRequest(
                    "Does every human deserve privacy?", "civil", List.copyOf(history), null);

            var response = generateHiddenMessage(request);

            System.out.println(response.text());
            history.add(response.text());
        }
    }

    @Test
    void excludesProhibitedMessagesThroughTheRealOpenAiEndpoint() {
        var prohibitedMessages = new ArrayList<>(List.of(
                "Каждый человек заслуживает права на личную жизнь.",
                "Да, каждый человек имеет право на приватность."));

        for (int i = 0; i < 3; i++) {
            // Keep the conversation unchanged so only the prohibited list grows.
            var request = new BuildHiddenMessageRequest(
                    "Does every human deserve privacy?", "civil", List.of(),
                    List.copyOf(prohibitedMessages));

            var response = generateHiddenMessage(request);

            assertFalse(request.prohibitedMessages().contains(response.text()),
                    () -> "Generated a prohibited message: " + response.text());
            prohibitedMessages.add(response.text());
        }
    }

    private HiddenMessageResponse generateHiddenMessage(BuildHiddenMessageRequest request) {
        HiddenMessageResponse response = client.post()
                .uri("/generate-hidden-message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(HiddenMessageResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.errorDetails(), () -> "Message generation failed: " + response.errorDetails());
        assertNotNull(response.text());
        assertFalse(response.text().isBlank());
        return response;
    }
}
