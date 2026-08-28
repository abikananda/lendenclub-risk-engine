package com.abikananda.lendenclub.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRiskResult {
    private Double riskScore;
    private RiskLevel riskLevel;
    private String rationale;
}
