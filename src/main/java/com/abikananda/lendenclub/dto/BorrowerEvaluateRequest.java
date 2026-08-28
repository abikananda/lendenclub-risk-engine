package com.abikananda.lendenclub.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerEvaluateRequest {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String loanId;

    @NotNull
    private Integer creditScore;

    @NotNull
    private Integer lendenScore;

    @NotNull @Positive
    private BigDecimal income;

    @NotNull @Positive
    private BigDecimal loanAmount;

    @NotNull @Positive
    private BigDecimal interestRate;

    @NotNull @Positive
    private Integer tenure;

    @NotNull @Positive
    private BigDecimal emi;

    @NotNull
    private Integer age;

    @NotBlank
    private String borrowerType;

    @NotNull
    private Boolean repeated;
}
