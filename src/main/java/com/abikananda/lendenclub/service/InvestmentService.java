package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.InvestmentStatus;
import com.abikananda.lendenclub.dto.InvestmentResponse;
import com.abikananda.lendenclub.dto.InvestmentStatusRequest;
import com.abikananda.lendenclub.dto.InvestmentSummaryResponse;
import com.abikananda.lendenclub.entity.Investment;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.repository.InvestmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class InvestmentService {

    private static final Logger log = LoggerFactory.getLogger(InvestmentService.class);

    private final InvestmentRepository investmentRepository;
    private final LendingSessionService sessionService;
    private final AuditService auditService;

    public InvestmentService(InvestmentRepository investmentRepository,
                             LendingSessionService sessionService,
                             AuditService auditService) {
        this.investmentRepository = investmentRepository;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    @Transactional
    public InvestmentResponse recordStatus(InvestmentStatusRequest req) {
        log.info("sessionId={} loanId={} Investment status update received: status={} extId={}",
                req.getSessionId(), req.getLoanId(), req.getStatus(), req.getExternalInvestmentId());

        // Lock the session row first. This serializes investment updates for one lending session,
        // preventing both duplicate inserts and lost counter updates under concurrent callbacks.
        LendingSession session = sessionService.requireActiveSessionForUpdate(req.getSessionId());

        if (req.getExternalInvestmentId() != null && !req.getExternalInvestmentId().isBlank()) {
            Optional<Investment> existing = investmentRepository.findByExternalInvestmentId(req.getExternalInvestmentId());
            if (existing.isPresent()) {
                Investment previous = existing.get();
                if (!previous.getSessionId().equals(req.getSessionId())) {
                    throw new IllegalStateException("External investment ID already belongs to a different session");
                }
                log.info("Idempotent duplicate ignored for externalInvestmentId={}", req.getExternalInvestmentId());
                return mapToResponse(previous);
            }
        }

        Investment investment = Investment.builder()
                .loanId(req.getLoanId())
                .sessionId(req.getSessionId())
                .lender(session.getLender())
                .requestedAmount(req.getInvestmentAmount())
                .status(req.getStatus())
                .externalInvestmentId(req.getExternalInvestmentId())
                .failureReason(req.getMessage())
                .completedAt(req.getStatus() == InvestmentStatus.SUCCESS ? OffsetDateTime.now() : null)
                .build();

        investment = investmentRepository.save(investment);

        boolean isSuccess = req.getStatus() == InvestmentStatus.SUCCESS;
        sessionService.recordInvestmentResult(session, isSuccess, req.getInvestmentAmount());

        auditService.logEvent("INVESTMENT_STATUS", req.getSessionId(), req.getLoanId(),
                "Status=" + req.getStatus() + " Amount=" + req.getInvestmentAmount());

        return mapToResponse(investment);
    }

    public InvestmentSummaryResponse getSummaryData() {
        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);

        return InvestmentSummaryResponse.builder()
                .totalInvestments(investmentRepository.count())
                .successfulInvestments(investmentRepository.countByStatus(InvestmentStatus.SUCCESS))
                .failedInvestments(investmentRepository.countByStatus(InvestmentStatus.FAILED))
                .pendingInvestments(investmentRepository.countByStatus(InvestmentStatus.INITIATED))
                .totalAmountInvested(investmentRepository.sumTotalSuccessfulAmount())
                .todayInvestmentCount(investmentRepository.countTodaySuccessful(startOfDay))
                .todayInvestmentAmount(investmentRepository.sumTodaySuccessfulAmount(startOfDay))
                .build();
    }

    public List<Investment> getInvestmentsForLoan(String loanId) {
        return investmentRepository.findByLoanId(loanId);
    }

    public Page<Investment> getRecentInvestments(Pageable pageable) {
        return investmentRepository.findAllByOrderByRequestedAtDesc(pageable);
    }

    private InvestmentResponse mapToResponse(Investment i) {
        return InvestmentResponse.builder()
                .id(i.getId())
                .loanId(i.getLoanId())
                .sessionId(i.getSessionId())
                .requestedAmount(i.getRequestedAmount())
                .status(i.getStatus())
                .externalInvestmentId(i.getExternalInvestmentId())
                .failureReason(i.getFailureReason())
                .requestedAt(i.getRequestedAt())
                .build();
    }
}
