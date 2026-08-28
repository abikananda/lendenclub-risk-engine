package com.abikananda.lendenclub.dto;

import com.abikananda.lendenclub.domain.InvestmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentResponse {
    private Long id;
    private String loanId;
    private String sessionId;
    private BigDecimal requestedAmount;
    private InvestmentStatus status;
    private String externalInvestmentId;
    private String failureReason;
    private OffsetDateTime requestedAt;
}
