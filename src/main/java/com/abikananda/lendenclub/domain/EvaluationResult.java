package com.abikananda.lendenclub.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {
    private LendingDecision decision;
    private RiskLevel riskLevel;
    private BigDecimal investmentAmount;
    private String ruleName;
    private String ruleCode;
    private String reason;
}
