package com.abikananda.lendenclub.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorrowerEvaluateRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequestPassesValidationWithOptionalProfileFieldsPresent() {
        BorrowerEvaluateRequest request = validRequest();
        request.setEmi(null); // The platform does not supply EMI.
        var violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidRequiredFinancialInputsAreRejected() {
        BorrowerEvaluateRequest request = validRequest();
        request.setSessionId(" ");
        request.setLoanId("");
        request.setIncome(BigDecimal.ZERO);
        request.setLoanAmount(new BigDecimal("-1"));
        request.setInterestRate(BigDecimal.ZERO);
        request.setTenure(0);
        request.setEmi(null);
        request.setBorrowerType(" ");
        request.setRepeated(null);

        var violations = validator.validate(request);

        assertEquals(8, violations.size());
    }

    private BorrowerEvaluateRequest validRequest() {
        return BorrowerEvaluateRequest.builder()
                .sessionId("SESSION-1")
                .loanId("LOAN-1")
                .borrowerName("Test Borrower")
                .creditScore(700)
                .lendenScore(800)
                .income(new BigDecimal("50000"))
                .loanAmount(new BigDecimal("5000"))
                .interestRate(new BigDecimal("36.48"))
                .tenure(4)
                .emi(new BigDecimal("1250"))
                .age(35)
                .borrowerType("SALARIED")
                .repeated(false)
                .loanType("PERSONAL")
                .repaymentFrequency("MONTHLY")
                .gender("FEMALE")
                .riskCategory("LOW")
                .build();
    }
}
