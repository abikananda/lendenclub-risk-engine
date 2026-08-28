package com.abikananda.lendenclub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentSummaryResponse {
    private long totalInvestments;
    private long successfulInvestments;
    private long failedInvestments;
    private long pendingInvestments;
    private BigDecimal totalAmountInvested;
    private long todayInvestmentCount;
    private BigDecimal todayInvestmentAmount;
}
