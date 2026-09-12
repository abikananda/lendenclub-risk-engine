package com.abikananda.lendenclub.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SeleniumBorrowerRecord(
        long id, String loanId, Integer creditScore, Integer lendenScore,
        BigDecimal income, BigDecimal loanAmount, String borrowerType,
        BigDecimal interestRate, String name, Integer age, BigDecimal lendingAmount,
        Integer tenure, String user, Instant createdDate) { }
