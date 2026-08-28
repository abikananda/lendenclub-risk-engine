package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.config.DroolsConfig;
import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.domain.EvaluationResult;
import com.abikananda.lendenclub.domain.LendingDecision;
import com.abikananda.lendenclub.domain.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DroolsEvaluationServiceTest {

    private DroolsEvaluationService droolsService;

    @BeforeEach
    void setUp() {
        DroolsConfig config = new DroolsConfig();
        droolsService = new DroolsEvaluationService(config.kieContainer());
    }

    @Test
    void testLowCreditScore_Rejection() {
        BorrowerFact fact = BorrowerFact.builder()
                .loanId("LOA-100")
                .creditScore(549)
                .lendenScore(800)
                .income(new BigDecimal("60000"))
                .loanAmount(new BigDecimal("10000"))
                .interestRate(new BigDecimal("20"))
                .tenure(6)
                .emi(new BigDecimal("2000"))
                .age(30)
                .borrowerType("SELF-EMPLOYED")
                .repeated(true)
                .build();

        EvaluationResult res = droolsService.evaluate(fact, "LS-TEST");
        assertEquals(LendingDecision.REJECT, res.getDecision());
        assertEquals(RiskLevel.HIGH, res.getRiskLevel());
        assertEquals("Reject - Low Credit Score", res.getRuleName());
    }

    @Test
    void testRepeatedLendersHighRisk_Success() {
        BorrowerFact fact = BorrowerFact.builder()
                .loanId("LOA-101")
                .creditScore(710)
                .lendenScore(780)
                .income(new BigDecimal("50000"))
                .loanAmount(new BigDecimal("15000"))
                .interestRate(new BigDecimal("22"))
                .tenure(6)
                .emi(new BigDecimal("2800"))
                .age(35)
                .borrowerType("SALARIED")
                .repeated(true)
                .build();

        EvaluationResult res = droolsService.evaluateSpecificRule(
                fact,
                "LS-TEST",
                "Repeated Lenders - High Risk"
        );
        assertEquals(LendingDecision.INVEST, res.getDecision());
        assertEquals(RiskLevel.HIGH, res.getRiskLevel());
        assertEquals("Repeated Lenders - High Risk", res.getRuleName());
        assertEquals(new BigDecimal("1000.00"), res.getInvestmentAmount());
    }

    @Test
    void testDefaultSkip() {
        BorrowerFact fact = BorrowerFact.builder()
                .loanId("LOA-102")
                .creditScore(680)
                .lendenScore(650)
                .income(new BigDecimal("40000"))
                .loanAmount(new BigDecimal("10000"))
                .interestRate(new BigDecimal("15"))
                .tenure(12)
                .emi(new BigDecimal("1000"))
                .age(25)
                .borrowerType("SALARIED")
                .repeated(false)
                .build();

        EvaluationResult res = droolsService.evaluate(fact, "LS-TEST");
        assertEquals(LendingDecision.SKIP, res.getDecision());
    }

    @Test
    void testBulkLenders_RepeatingDecimalRatio_DoesNotThrow() {
        BorrowerFact fact = BorrowerFact.builder()
                .loanId("LOA-RATIO-BULK")
                .creditScore(620)
                .lendenScore(760)
                .income(new BigDecimal("93566"))
                .loanAmount(new BigDecimal("5500"))
                .interestRate(new BigDecimal("36.48"))
                .tenure(4)
                .emi(new BigDecimal("1375"))
                .age(47)
                .borrowerType("SALARIED")
                .repeated(false)
                .build();

        EvaluationResult res = assertDoesNotThrow(
                () -> droolsService.evaluateSpecificRule(fact, "LS-TEST", "Bulk Lenders")
        );

        assertEquals(LendingDecision.INVEST, res.getDecision());
        assertEquals("Bulk Lenders", res.getRuleName());
        assertEquals(new BigDecimal("250.00"), res.getInvestmentAmount());
    }

    @Test
    void testRepeatedLenders_RepeatingDecimalRatio_DoesNotThrow() {
        BorrowerFact fact = BorrowerFact.builder()
                .loanId("LOA-RATIO-REPEATED")
                .creditScore(701)
                .lendenScore(800)
                .income(new BigDecimal("25026"))
                .loanAmount(new BigDecimal("4000"))
                .interestRate(new BigDecimal("36.48"))
                .tenure(4)
                .emi(new BigDecimal("1000"))
                .age(42)
                .borrowerType("SALARIED")
                .repeated(true)
                .build();

        EvaluationResult res = assertDoesNotThrow(
                () -> droolsService.evaluateSpecificRule(fact, "LS-TEST", "Repeated Lenders - High Risk")
        );

        assertEquals(LendingDecision.INVEST, res.getDecision());
        assertEquals("Repeated Lenders - High Risk", res.getRuleName());
        assertEquals(new BigDecimal("1000.00"), res.getInvestmentAmount());
    }
}
