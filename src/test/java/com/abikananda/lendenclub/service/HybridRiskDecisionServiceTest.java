package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class HybridRiskDecisionServiceTest {

    @Test
    void shadowNeverChangesDroolsDecision() {
        EvaluationResult drools = invest("100");
        EvaluationResult result = new HybridRiskDecisionService("shadow").apply(drools, ai(AiRecommendation.REJECT, null));
        assertSame(drools, result);
    }

    @Test
    void guardrailNeverIncreasesDroolsAmount() {
        EvaluationResult result = new HybridRiskDecisionService("guardrail")
                .apply(invest("100"), ai(AiRecommendation.REDUCE, new BigDecimal("200")));
        assertEquals(new BigDecimal("100"), result.getInvestmentAmount());
    }

    @Test
    void guardrailReducesAmount() {
        EvaluationResult result = new HybridRiskDecisionService("guardrail")
                .apply(invest("100"), ai(AiRecommendation.REDUCE, new BigDecimal("40")));
        assertEquals(LendingDecision.INVEST, result.getDecision());
        assertEquals(new BigDecimal("40"), result.getInvestmentAmount());
    }

    @Test
    void guardrailSkipsWhenProviderFails() {
        AiRiskResult ai = AiRiskResult.builder().status(AiRiskStatus.PROVIDER_ERROR).build();
        EvaluationResult result = new HybridRiskDecisionService("guardrail").apply(invest("100"), ai);
        assertEquals(LendingDecision.SKIP, result.getDecision());
        assertEquals(BigDecimal.ZERO, result.getInvestmentAmount());
    }

    @Test
    void guardrailCannotOverrideDroolsRejection() {
        EvaluationResult rejected = EvaluationResult.builder().decision(LendingDecision.REJECT)
                .riskLevel(RiskLevel.HIGH).investmentAmount(BigDecimal.ZERO).build();
        assertSame(rejected, new HybridRiskDecisionService("guardrail")
                .apply(rejected, ai(AiRecommendation.APPROVE, new BigDecimal("1000"))));
    }

    private EvaluationResult invest(String amount) {
        return EvaluationResult.builder().decision(LendingDecision.INVEST).riskLevel(RiskLevel.LOW)
                .investmentAmount(new BigDecimal(amount)).reason("Drools approved").build();
    }

    private AiRiskResult ai(AiRecommendation recommendation, BigDecimal maximum) {
        return AiRiskResult.builder().status(AiRiskStatus.COMPLETED).riskLevel(RiskLevel.MEDIUM)
                .recommendation(recommendation).maximumRecommendedAmount(maximum).rationale("reviewed").build();
    }
}
