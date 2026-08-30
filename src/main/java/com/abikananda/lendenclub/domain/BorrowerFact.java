package com.abikananda.lendenclub.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerFact {
    private String loanId;
    private Integer creditScore;
    private Integer lendenScore;
    private BigDecimal income;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenure;
    private BigDecimal emi;
    private Integer age;
    private String borrowerType;
    private Boolean repeated;
    private Boolean trusted;
}
