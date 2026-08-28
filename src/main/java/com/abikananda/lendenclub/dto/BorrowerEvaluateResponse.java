package com.abikananda.lendenclub.dto;

import com.abikananda.lendenclub.domain.LendingDecision;
import com.abikananda.lendenclub.domain.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerEvaluateResponse {
    private String loanId;
    private String sessionId;
    private LendingDecision decision;
    private RiskLevel riskLevel;
    private BigDecimal investmentAmount;
    private String rule;
    private String reason;
    private Long evaluationId;
}
