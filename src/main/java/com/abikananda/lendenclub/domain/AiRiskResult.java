package com.abikananda.lendenclub.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRiskResult {
    private Double riskScore;
    private RiskLevel riskLevel;
    private AiRecommendation recommendation;
    private Double confidence;
    private BigDecimal maximumRecommendedAmount;
    private List<String> concerns;
    private List<String> positiveFactors;
    private String rationale;
    private String provider;
    private String model;
    private String promptVersion;
    private AiRiskStatus status;
    private Long latencyMs;
}
