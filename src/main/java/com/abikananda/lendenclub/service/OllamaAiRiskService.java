package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnExpression("'${risk-engine.ai.enabled:false}' == 'true' && '${risk-engine.ai.provider:noop}' == 'ollama'")
public class OllamaAiRiskService implements AiRiskService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiRiskService.class);
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String promptVersion;

    public OllamaAiRiskService(ObjectMapper objectMapper,
                               @Value("${risk-engine.ai.base-url:http://localhost:11434}") String baseUrl,
                               @Value("${risk-engine.ai.model:llama3.2:3b}") String model,
                               @Value("${risk-engine.ai.timeout:10s}") Duration timeout,
                               @Value("${risk-engine.ai.prompt-version:v1}") String promptVersion) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(timeout);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.promptVersion = promptVersion;
    }

    @Override
    public AiRiskResult evaluate(BorrowerFact borrower) {
        long started = System.nanoTime();
        try {
            OllamaResponse response = client.post().uri("/api/generate")
                    .body(Map.of(
                            "model", model,
                            "prompt", prompt(borrower),
                            "stream", false,
                            "format", "json",
                            "options", Map.of("temperature", 0)))
                    .retrieve().body(OllamaResponse.class);
            if (response == null || response.response() == null) {
                return failure(AiRiskStatus.INVALID_RESPONSE, "Ollama returned an empty response", started);
            }
            AiPayload payload = objectMapper.readValue(response.response(), AiPayload.class);
            validate(payload);
            return AiRiskResult.builder()
                    .riskScore(payload.riskScore)
                    .riskLevel(payload.riskLevel)
                    .recommendation(payload.recommendation)
                    .confidence(payload.confidence)
                    .maximumRecommendedAmount(payload.maximumRecommendedAmount)
                    .concerns(payload.concerns)
                    .positiveFactors(payload.positiveFactors)
                    .rationale(payload.rationale)
                    .provider("ollama")
                    .model(model)
                    .promptVersion(promptVersion)
                    .status(AiRiskStatus.COMPLETED)
                    .latencyMs(elapsed(started))
                    .build();
        } catch (Exception e) {
            log.warn("AI risk assessment failed provider=ollama model={} error={}", model, e.getMessage());
            return failure(AiRiskStatus.PROVIDER_ERROR, "AI provider unavailable", started);
        }
    }

    private void validate(AiPayload p) {
        if (p.riskScore == null || p.riskScore < 0 || p.riskScore > 100
                || p.riskLevel == null || p.recommendation == null
                || p.confidence == null || p.confidence < 0 || p.confidence > 1
                || p.rationale == null || p.rationale.isBlank()) {
            throw new IllegalArgumentException("Invalid structured AI response");
        }
        if (p.recommendation == AiRecommendation.REDUCE
                && (p.maximumRecommendedAmount == null || p.maximumRecommendedAmount.signum() <= 0)) {
            throw new IllegalArgumentException("REDUCE requires a positive maximumRecommendedAmount");
        }
    }

    private String prompt(BorrowerFact b) {
        return """
                You are a conservative lending risk reviewer. Assess only the supplied numeric and categorical data.
                Do not invent facts. Return JSON only with: riskScore (0-100), riskLevel
                (LOW|MEDIUM|HIGH|UNKNOWN), recommendation (APPROVE|REDUCE|REVIEW|REJECT),
                confidence (0-1), maximumRecommendedAmount (number or null), concerns (string array),
                positiveFactors (string array), rationale (short string).
                A recommendation never authorizes more than the deterministic rule engine.
                Data: creditScore=%s, lendenScore=%s, income=%s, loanAmount=%s, interestRate=%s,
                tenure=%s, emi=%s, age=%s, borrowerType=%s, repeated=%s, trusted=%s
                """.formatted(b.getCreditScore(), b.getLendenScore(), b.getIncome(), b.getLoanAmount(),
                b.getInterestRate(), b.getTenure(), b.getEmi(), b.getAge(), b.getBorrowerType(),
                b.getRepeated(), b.getTrusted());
    }

    private AiRiskResult failure(AiRiskStatus status, String rationale, long started) {
        return AiRiskResult.builder().riskLevel(RiskLevel.UNKNOWN).rationale(rationale)
                .provider("ollama").model(model).promptVersion(promptVersion)
                .status(status).latencyMs(elapsed(started)).build();
    }

    private long elapsed(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    private record OllamaResponse(String response) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiPayload {
        public Double riskScore;
        public RiskLevel riskLevel;
        public AiRecommendation recommendation;
        public Double confidence;
        public BigDecimal maximumRecommendedAmount;
        public List<String> concerns;
        public List<String> positiveFactors;
        public String rationale;
    }
}
