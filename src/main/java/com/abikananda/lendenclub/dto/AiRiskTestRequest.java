package com.abikananda.lendenclub.dto;

import com.abikananda.lendenclub.domain.BorrowerFact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRiskTestRequest {

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

    // Optional legacy input; the derived monthly principal payment is authoritative.
    private BigDecimal emi;

    @NotNull @Positive
    private Integer age;

    @NotBlank
    private String borrowerType;

    @NotNull
    private Boolean repeated;

    @NotNull
    private Boolean trusted;

    public BorrowerFact toFact() {
        return BorrowerFact.builder()
                .creditScore(creditScore)
                .lendenScore(lendenScore)
                .income(income)
                .loanAmount(loanAmount)
                .interestRate(interestRate)
                .tenure(tenure)
                .emi(loanAmount.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP))
                .age(age)
                .borrowerType(borrowerType)
                .repeated(repeated)
                .trusted(trusted)
                .build();
    }
}
