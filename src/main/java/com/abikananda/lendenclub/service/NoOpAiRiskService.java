package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.AiRiskResult;
import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.domain.RiskLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "risk-engine.ai.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAiRiskService implements AiRiskService {

    @Override
    public AiRiskResult evaluate(BorrowerFact borrower) {
        return AiRiskResult.builder()
                .riskScore(0.0)
                .riskLevel(RiskLevel.UNKNOWN)
                .rationale("AI Risk Engine Disabled")
                .build();
    }
}
