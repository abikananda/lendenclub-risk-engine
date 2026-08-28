package com.abikananda.lendenclub.dto;

import com.abikananda.lendenclub.domain.LendingSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LendingSessionResponse {
    private String sessionId;
    private LendingSessionStatus status;
    private Integer totalBorrowersEvaluated;
    private Integer totalInvestments;
    private BigDecimal totalAmountInvested;
}
