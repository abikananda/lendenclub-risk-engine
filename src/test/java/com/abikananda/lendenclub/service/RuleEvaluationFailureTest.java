package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.exception.RuleEvaluationException;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEvaluationFailureTest {

    @Test
    void wrapsSpecificRuleFailureWithStableDomainException() {
        KieContainer container = mock(KieContainer.class);
        KieSession session = mock(KieSession.class);
        when(container.newKieSession()).thenReturn(session);
        doThrow(new ArithmeticException("simulated rule arithmetic failure"))
                .when(session).fireAllRules(any());

        DroolsEvaluationService service = new DroolsEvaluationService(container);
        BorrowerFact fact = BorrowerFact.builder()
                .loanId("LOAN-FAILURE-1")
                .creditScore(700)
                .lendenScore(750)
                .income(new BigDecimal("93566"))
                .loanAmount(new BigDecimal("5500"))
                .interestRate(new BigDecimal("36.48"))
                .tenure(7)
                .emi(new BigDecimal("1000"))
                .age(35)
                .borrowerType("SALARIED")
                .repeated(false)
                .build();

        RuleEvaluationException ex = assertThrows(
                RuleEvaluationException.class,
                () -> service.evaluateSpecificRule(fact, "LS-TEST", "Bulk Lenders"));

        assertEquals("LOAN-FAILURE-1", ex.getLoanId());
        assertEquals("Bulk Lenders", ex.getRuleName());
    }
}
