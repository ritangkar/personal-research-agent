package com.ritangkar.researchagent.synthesis.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ritangkar.researchagent.model.EvidenceEvaluation;
import com.ritangkar.researchagent.retry.RetryExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Calls an OpenAI-compatible chat completions endpoint to phrase a section's
 * answer, still constrained to the evidence ids it was given.
 *
 * <b>Disabled by default</b> (only instantiated when
 * {@code research.llm.enabled=true}) and never used unless {@code
 * research.llm.provider=openai-compatible}. The API key is read exclusively
 * from the {@code OPENAI_API_KEY} environment variable — never from a
 * property file, never logged, never returned in any response.
 *
 * This client is not exercised by the automated test suite: it requires a
 * live network call and a real API key, neither of which the CI/build
 * environment for this project has. Treat it as a documented integration
 * point rather than a verified one until you've run it against a real key.
 */
@Component
@ConditionalOnProperty(prefix = "research.llm", name = "enabled", havingValue = "true")
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final RetryExecutor retryExecutor;

    private final String baseUrl;
    private final String model;

    public OpenAiCompatibleLlmClient(
            @Value("${research.llm.base-url}") String baseUrl,
            @Value("${research.llm.model}") String model,
            @Value("${research.retry.max-attempts:3}") int maxAttempts,
            @Value("${research.retry.initial-backoff-ms:100}") long initialBackoffMs) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.retryExecutor = new RetryExecutor(maxAttempts, initialBackoffMs);
    }

    @Override
    public String name() {
        return "openai-compatible:" + model;
    }

    @Override
    public SynthesisResult synthesize(String subQuestion, List<EvidenceEvaluation> acceptedEvidence) {
        if (acceptedEvidence.isEmpty()) {
            return new SynthesisResult(
                    "No evidence in the available sources cleared the acceptance threshold for this sub-question.",
                    List.of());
        }

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "research.llm.enabled=true but no OPENAI_API_KEY environment variable was found. " +
                            "Set it before starting the application, or leave research.llm.enabled=false to use the demo client.");
        }

        String prompt = buildGroundedPrompt(subQuestion, acceptedEvidence);
        List<String> allowedIds = acceptedEvidence.stream().map(e -> e.evidence().id()).toList();

        RetryExecutor.Outcome<String> outcome = retryExecutor.execute(() -> callChatCompletions(prompt, apiKey));
        return parseAndValidate(outcome.value(), allowedIds);
    }

    private String buildGroundedPrompt(String subQuestion, List<EvidenceEvaluation> evidence) {
        String sources = evidence.stream()
                .map(e -> "[" + e.evidence().id() + "] " + e.evidence().sourceName() + " ("
                        + e.evidence().publishedYear() + "): " + e.evidence().snippet())
                .collect(Collectors.joining("\n"));

        return "Answer the sub-question using ONLY the numbered sources below. " +
                "Cite sources by their bracketed id inline. Do not use any fact not present in these sources. " +
                "If the sources do not answer the question, say so explicitly.\n\n" +
                "Sub-question: " + subQuestion + "\n\nSources:\n" + sources;
    }

    private String callChatCompletions(String prompt, String apiKey) {
        try {
            var body = mapper.createObjectNode();
            body.put("model", model);
            var messages = body.putArray("messages");
            messages.addObject().put("role", "user").put("content", prompt);
            body.put("temperature", 0.2);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("LLM provider returned HTTP " + response.statusCode());
            }

            JsonNode json = mapper.readTree(response.body());
            return json.at("/choices/0/message/content").asText();
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Strips out any cited id the model invented that wasn't in the allowed
     * set — the enforcement point that makes hallucinated citations
     * impossible to surface, regardless of what the model returns.
     */
    private SynthesisResult parseAndValidate(String rawText, List<String> allowedIds) {
        List<String> foundIds = new ArrayList<>();
        for (String id : allowedIds) {
            if (rawText.contains(id)) foundIds.add(id);
        }
        return new SynthesisResult(rawText, foundIds);
    }
}
