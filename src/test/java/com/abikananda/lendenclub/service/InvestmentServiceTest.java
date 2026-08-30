package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.InvestmentStatus;
import com.abikananda.lendenclub.dto.InvestmentStatusRequest;
import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.entity.Investment;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import com.abikananda.lendenclub.repository.InvestmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;
    @Mock
    private BorrowerSnapshotRepository snapshotRepository;
    @Mock
    private LendingSessionService sessionService;
    @Mock
    private BorrowerIdentityService borrowerIdentityService;
    @Mock
    private AuditService auditService;

    private InvestmentService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentService(
                investmentRepository,
                snapshotRepository,
                sessionService,
                borrowerIdentityService,
                auditService);
    }

    @Test
    void successfulStatusPersistsInvestmentAndUpdatesBorrowerLifetimeOnce() {
        Lender lender = Lender.builder().id(10L).username("test-user").build();
        LendingSession session = LendingSession.builder().sessionId("SESSION-1").lender(lender).build();
        BorrowerProfile profile = BorrowerProfile.builder()
                .id(7L)
                .publicId("profile-1")
                .displayName("Test Borrower")
                .normalizedName("test borrower")
                .totalLent(BigDecimal.ZERO)
                .successfulInvestmentCount(0L)
                .build();
        BorrowerSnapshot snapshot = BorrowerSnapshot.builder().borrowerProfile(profile).build();

        when(sessionService.requireActiveSessionForUpdate("SESSION-1")).thenReturn(session);
        when(investmentRepository.findFirstBySessionIdAndLoanIdAndStatusOrderByRequestedAtDesc(
                "SESSION-1", "LOAN-1", InvestmentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(investmentRepository.findByExternalInvestmentId("EXT-1")).thenReturn(Optional.empty());
        when(snapshotRepository.findTopBySessionIdAndLoanIdOrderByScrapedAtDesc("SESSION-1", "LOAN-1"))
                .thenReturn(Optional.of(snapshot));
        when(investmentRepository.save(any())).thenAnswer(invocation -> {
            Investment investment = invocation.getArgument(0);
            investment.setId(55L);
            return investment;
        });

        var response = service.recordStatus(request("SESSION-1", "EXT-1", InvestmentStatus.SUCCESS));

        assertEquals(55L, response.getId());
        assertEquals(InvestmentStatus.SUCCESS, response.getStatus());

        ArgumentCaptor<Investment> captor = ArgumentCaptor.forClass(Investment.class);
        verify(investmentRepository).save(captor.capture());
        Investment saved = captor.getValue();
        assertEquals("LOAN-1", saved.getLoanId());
        assertEquals(lender, saved.getLender());
        assertEquals(profile, saved.getBorrowerProfile());
        assertEquals(0, new BigDecimal("250.00").compareTo(saved.getRequestedAmount()));

        verify(sessionService).recordInvestmentResult(session, true, new BigDecimal("250.00"));
        verify(borrowerIdentityService).recordSuccessfulInvestment(profile, new BigDecimal("250.00"));
        verify(auditService).logEvent("INVESTMENT_STATUS", "SESSION-1", "LOAN-1", "Status=SUCCESS Amount=250.00");
    }

    @Test
    void duplicateSuccessForSameSessionAndLoanIsIdempotent() {
        Lender lender = Lender.builder().id(10L).username("test-user").build();
        LendingSession session = LendingSession.builder().sessionId("SESSION-1").lender(lender).build();
        Investment existing = Investment.builder()
                .id(99L)
                .loanId("LOAN-1")
                .sessionId("SESSION-1")
                .lender(lender)
                .requestedAmount(new BigDecimal("250.00"))
                .status(InvestmentStatus.SUCCESS)
                .externalInvestmentId("EXT-1")
                .build();

        when(sessionService.requireActiveSessionForUpdate("SESSION-1")).thenReturn(session);
        when(investmentRepository.findFirstBySessionIdAndLoanIdAndStatusOrderByRequestedAtDesc(
                "SESSION-1", "LOAN-1", InvestmentStatus.SUCCESS)).thenReturn(Optional.of(existing));

        var response = service.recordStatus(request("SESSION-1", "EXT-1", InvestmentStatus.SUCCESS));

        assertEquals(99L, response.getId());
        verify(investmentRepository, never()).save(any());
        verify(sessionService, never()).recordInvestmentResult(isA(LendingSession.class), anyBoolean(), isA(BigDecimal.class));
        verify(borrowerIdentityService, never()).recordSuccessfulInvestment(any(), any());
        verify(auditService, never()).logEvent(any(), any(), any(), any());
    }

    @Test
    void duplicateExternalIdFromDifferentSessionIsRejected() {
        LendingSession session = LendingSession.builder()
                .sessionId("SESSION-2")
                .lender(Lender.builder().id(20L).build())
                .build();
        Investment existing = Investment.builder()
                .sessionId("SESSION-1")
                .externalInvestmentId("EXT-1")
                .build();

        when(sessionService.requireActiveSessionForUpdate("SESSION-2")).thenReturn(session);
        when(investmentRepository.findFirstBySessionIdAndLoanIdAndStatusOrderByRequestedAtDesc(
                "SESSION-2", "LOAN-1", InvestmentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(investmentRepository.findByExternalInvestmentId("EXT-1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> service.recordStatus(request("SESSION-2", "EXT-1", InvestmentStatus.SUCCESS)));

        verify(investmentRepository, never()).save(any());
        verify(sessionService, never()).recordInvestmentResult(isA(LendingSession.class), anyBoolean(), isA(BigDecimal.class));
    }

    private InvestmentStatusRequest request(String sessionId, String externalId, InvestmentStatus status) {
        return InvestmentStatusRequest.builder()
                .sessionId(sessionId)
                .loanId("LOAN-1")
                .investmentAmount(new BigDecimal("250.00"))
                .status(status)
                .externalInvestmentId(externalId)
                .message("ok")
                .build();
    }
}
