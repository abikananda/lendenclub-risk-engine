package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.EvaluationResult;
import com.abikananda.lendenclub.domain.RiskLevel;
import com.abikananda.lendenclub.dto.BorrowerEvaluateRequest;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.repository.BorrowerEvaluationRepository;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerEvaluationServiceTest {

    @Mock
    private LendingSessionService sessionService;
    @Mock
    private DroolsEvaluationService droolsService;
    @Mock
    private AiRiskService aiRiskService;
    @Mock
    private BorrowerSnapshotRepository snapshotRepository;
    @Mock
    private BorrowerEvaluationRepository evaluationRepository;
    @Mock
    private AuditService auditService;

    private BorrowerEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new BorrowerEvaluationService(
                sessionService,
                droolsService,
                aiRiskService,
                snapshotRepository,
                evaluationRepository,
                auditService,
                new ObjectMapper(),
                "test-engine");
    }

    @Test
    void noMatchStillPersistsCompleteBorrowerSnapshotButDoesNotRunAiOrSaveEvaluation() {
        BorrowerEvaluateRequest request = request();
        when(droolsService.evaluateSpecificRule(any(), eq("Bulk Lenders")))
                .thenReturn(EvaluationResult.builder()
                        .decision(null)
                        .riskLevel(RiskLevel.HIGH)
                        .ruleName(null)
                        .reason("No match")
                        .build());

        var response = service.evaluateSpecificRule(request, "Bulk Lenders");

        assertNull(response.getDecision());
        assertNull(response.getEvaluationId());
        verify(sessionService).validateAndTouchSession("SESSION-1");

        ArgumentCaptor<BorrowerSnapshot> snapshotCaptor = ArgumentCaptor.forClass(BorrowerSnapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());
        BorrowerSnapshot snapshot = snapshotCaptor.getValue();

        assertEquals("LOAN-1", snapshot.getLoanId());
        assertEquals("Test Borrower", snapshot.getBorrowerName());
        assertEquals("PERSONAL", snapshot.getLoanType());
        assertEquals("MONTHLY", snapshot.getRepaymentFrequency());
        assertEquals("FEMALE", snapshot.getGender());
        assertEquals("LOW", snapshot.getRiskCategory());
        assertEquals(701, snapshot.getCreditScore());
        assertEquals(800, snapshot.getLendenScore());
        assertEquals(0, new BigDecimal("50000").compareTo(snapshot.getIncome()));

        verify(aiRiskService, never()).evaluate(any());
        verify(evaluationRepository, never()).save(any());
    }

    private BorrowerEvaluateRequest request() {
        return BorrowerEvaluateRequest.builder()
                .sessionId("SESSION-1")
                .loanId("LOAN-1")
                .borrowerName("Test Borrower")
                .creditScore(701)
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
