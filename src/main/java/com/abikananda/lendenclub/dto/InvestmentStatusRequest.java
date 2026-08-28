package com.abikananda.lendenclub.dto;

import com.abikananda.lendenclub.domain.InvestmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentStatusRequest {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String loanId;

    @NotNull @Positive
    private BigDecimal investmentAmount;

    @NotNull
    private InvestmentStatus status;

    private String externalInvestmentId;
    private String message;
}
