package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class HybridRiskDecisionService {

    private final AiMode mode;

    public HybridRiskDecisionService(@Value("${risk-engine.ai.mode:off}") String mode) {
        this.mode = AiMode.from(mode);
    }

    public EvaluationResult apply(EvaluationResult drools, AiRiskResult ai) {
        if (mode != AiMode.GUARDRAIL || drools.getDecision() != LendingDecision.INVEST) {
            return drools;
        }
        if (ai == null || ai.getStatus() != AiRiskStatus.COMPLETED) {
            return blocked(drools, "AI guardrail requires manual review because the AI assessment was unavailable");
        }

        AiRecommendation recommendation = ai.getRecommendation();
        if (recommendation == AiRecommendation.REJECT || recommendation == AiRecommendation.REVIEW
                || ai.getRiskLevel() == RiskLevel.HIGH) {
            return blocked(drools, "AI guardrail requires manual review: " + safe(ai.getRationale()));
        }

        if (recommendation == AiRecommendation.REDUCE) {
            BigDecimal maximum = ai.getMaximumRecommendedAmount();
            if (maximum == null || maximum.signum() <= 0) {
                return blocked(drools, "AI guardrail requested a reduction without a valid maximum amount");
            }
            BigDecimal reduced = drools.getInvestmentAmount().min(maximum);
            return copy(drools, LendingDecision.INVEST, reduced,
                    drools.getReason() + " | AI guardrail reduced amount: " + safe(ai.getRationale()));
        }
        return drools;
    }

    private EvaluationResult blocked(EvaluationResult source, String reason) {
        return copy(source, LendingDecision.SKIP, BigDecimal.ZERO, reason);
    }

    private EvaluationResult copy(EvaluationResult source, LendingDecision decision,
                                  BigDecimal amount, String reason) {
        return EvaluationResult.builder()
                .decision(decision)
                .riskLevel(source.getRiskLevel())
                .investmentAmount(amount)
                .ruleName(source.getRuleName())
                .ruleCode(source.getRuleCode())
                .reason(reason)
                .build();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "no rationale supplied" : value;
    }
}
