package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.EvaluationResult;
import com.abikananda.lendenclub.domain.RiskLevel;
import com.abikananda.lendenclub.domain.LendingDecision;
import com.abikananda.lendenclub.domain.AiRiskResult;
import com.abikananda.lendenclub.dto.BorrowerEvaluateRequest;
import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
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

    @Mock private LendingSessionService sessionService;
    @Mock private DroolsEvaluationService droolsService;
    @Mock private AiRiskService aiRiskService;
    @Mock private BorrowerSnapshotRepository snapshotRepository;
    @Mock private BorrowerEvaluationRepository evaluationRepository;
    @Mock private BorrowerIdentityService borrowerIdentityService;
    @Mock private TrustedBorrowerService trustedBorrowerService;
    @Mock private AuditService auditService;

    private BorrowerEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new BorrowerEvaluationService(
                sessionService,
                droolsService,
                aiRiskService,
                new HybridRiskDecisionService("off"),
                snapshotRepository,
                evaluationRepository,
                borrowerIdentityService,
                trustedBorrowerService,
                auditService,
                new ObjectMapper(),
                "test-engine");
    }

    @Test
    void noMatchStillPersistsCompleteBorrowerSnapshotButDoesNotRunAiOrSaveEvaluation() {
        BorrowerEvaluateRequest request = request();
        BorrowerProfile profile = profile();
        when(borrowerIdentityService.resolveOrCreate("Test Borrower", "FEMALE", "SALARIED", 35)).thenReturn(profile);
        when(droolsService.evaluateSpecificRule(any(), eq("SESSION-1"), eq("Bulk Lenders")))
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
        assertEquals(profile, snapshot.getBorrowerProfile());
        assertEquals("PERSONAL", snapshot.getLoanType());
        assertEquals("MONTHLY", snapshot.getRepaymentFrequency());
        assertEquals("FEMALE", snapshot.getGender());
        assertEquals("LOW", snapshot.getRiskCategory());
        assertEquals(701, snapshot.getCreditScore());
        assertEquals(800, snapshot.getLendenScore());
        assertEquals(0, new BigDecimal("50000").compareTo(snapshot.getIncome()));

        verify(aiRiskService, never()).evaluate(any());
        verify(evaluationRepository, never()).save(any());
        verify(trustedBorrowerService, never()).isTrusted(any());
    }

    @Test
    void trustedRuleDerivesTrustedFlagFromRegistryInsteadOfRequestPayload() {
        BorrowerEvaluateRequest request = request();
        request.setTrusted(false);
        BorrowerProfile profile = profile();
        when(borrowerIdentityService.resolveOrCreate("Test Borrower", "FEMALE", "SALARIED", 35)).thenReturn(profile);
        when(trustedBorrowerService.isTrusted(profile)).thenReturn(true);
        when(droolsService.evaluateSpecificRule(any(), eq("SESSION-1"), eq("Trusted Lenders - Low Risk")))
                .thenReturn(EvaluationResult.builder().decision(null).reason("No match").build());

        service.evaluateSpecificRule(request, "Trusted Lenders - Low Risk");

        ArgumentCaptor<com.abikananda.lendenclub.domain.BorrowerFact> factCaptor =
                ArgumentCaptor.forClass(com.abikananda.lendenclub.domain.BorrowerFact.class);
        verify(droolsService).evaluateSpecificRule(factCaptor.capture(), eq("SESSION-1"), eq("Trusted Lenders - Low Risk"));
        assertEquals(true, factCaptor.getValue().getTrusted());
    }

    @Test
    void repeatedApprovalAcrossRulesReturnsSkipWithoutSavingAnotherApproval() {
        BorrowerEvaluateRequest request = request();
        request.setEmi(new BigDecimal("99999"));
        request.setRepaymentFrequency(" daily ");
        when(sessionService.validateAndTouchSession("SESSION-1"))
                .thenReturn(LendingSession.builder().lender(Lender.builder().id(10L).build()).build());
        when(borrowerIdentityService.resolveOrCreate("Test Borrower", "FEMALE", "SALARIED", 35))
                .thenReturn(profile());
        when(droolsService.evaluateSpecificRule(any(), eq("SESSION-1"), eq("Daily Repayment Lenders")))
                .thenReturn(EvaluationResult.builder().decision(LendingDecision.INVEST)
                        .riskLevel(RiskLevel.MEDIUM).investmentAmount(new BigDecimal("2000"))
                        .ruleName("Daily Repayment Lenders").build());
        when(aiRiskService.evaluate(any())).thenReturn(AiRiskResult.builder().build());

        var response = service.evaluateSpecificRule(request, "Daily Repayment Lenders");

        assertEquals(LendingDecision.SKIP, response.getDecision());
        assertEquals(BigDecimal.ZERO, response.getInvestmentAmount());
        verify(evaluationRepository).claimLoan(10L, "LOAN-1", "SESSION-1");
        verify(evaluationRepository, never()).save(any());
        ArgumentCaptor<com.abikananda.lendenclub.domain.BorrowerFact> fact =
                ArgumentCaptor.forClass(com.abikananda.lendenclub.domain.BorrowerFact.class);
        verify(droolsService).evaluateSpecificRule(fact.capture(), eq("SESSION-1"), eq("Daily Repayment Lenders"));
        assertEquals("DAILY", fact.getValue().getRepaymentFrequency());
        assertEquals(0, new BigDecimal("1250").compareTo(fact.getValue().getEmi()));
    }

    @Test
    void firstApprovalClaimsLenderLoanBeforeReturningInvest() {
        BorrowerEvaluateRequest request = request();
        request.setEmi(null);
        when(sessionService.validateAndTouchSession("SESSION-1"))
                .thenReturn(LendingSession.builder().lender(Lender.builder().id(10L).build()).build());
        when(borrowerIdentityService.resolveOrCreate("Test Borrower", "FEMALE", "SALARIED", 35))
                .thenReturn(profile());
        when(droolsService.evaluateSpecificRule(any(), eq("SESSION-1"), eq("Bulk Lenders")))
                .thenReturn(EvaluationResult.builder().decision(LendingDecision.INVEST)
                        .riskLevel(RiskLevel.HIGH).investmentAmount(new BigDecimal("250"))
                        .ruleName("Bulk Lenders").build());
        when(aiRiskService.evaluate(any())).thenReturn(AiRiskResult.builder().build());
        when(evaluationRepository.claimLoan(10L, "LOAN-1", "SESSION-1")).thenReturn(1);
        when(evaluationRepository.save(any())).thenAnswer(invocation -> {
            com.abikananda.lendenclub.entity.BorrowerEvaluation evaluation = invocation.getArgument(0);
            evaluation.setId(42L);
            return evaluation;
        });

        var response = service.evaluateSpecificRule(request, "Bulk Lenders");

        assertEquals(LendingDecision.INVEST, response.getDecision());
        assertEquals(42L, response.getEvaluationId());
        verify(evaluationRepository).claimLoan(10L, "LOAN-1", "SESSION-1");
    }

    private BorrowerProfile profile() {
        return BorrowerProfile.builder()
                .id(10L)
                .publicId("profile-1")
                .displayName("Test Borrower")
                .normalizedName("test borrower")
                .borrowerTypeNormalized("salaried")
                .totalLent(BigDecimal.ZERO)
                .successfulInvestmentCount(0L)
                .build();
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
