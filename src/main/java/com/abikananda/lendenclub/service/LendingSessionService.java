package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.LendingSessionStatus;
import com.abikananda.lendenclub.dto.LendingSessionResponse;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.exception.InvalidSessionException;
import com.abikananda.lendenclub.exception.ResourceNotFoundException;
import com.abikananda.lendenclub.repository.LendingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class LendingSessionService {

    private static final Logger log = LoggerFactory.getLogger(LendingSessionService.class);
    private final LendingSessionRepository sessionRepository;

    public LendingSessionService(LendingSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public LendingSession createSession(Lender lender) {
        String sessionId = "LS-" + UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();

        LendingSession session = LendingSession.builder()
                .sessionId(sessionId)
                .lender(lender)
                .status(LendingSessionStatus.STARTED)
                .startedAt(now)
                .lastActivityAt(now)
                .build();

        log.info("Creating new lending session sessionId={} for lenderId={}", sessionId, lender.getExternalLenderId());
        return sessionRepository.save(session);
    }

    @Transactional
    public LendingSession validateAndTouchSession(String sessionId) {
        LendingSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InvalidSessionException("Session not found: " + sessionId));

        if (session.getStatus() == LendingSessionStatus.COMPLETED ||
            session.getStatus() == LendingSessionStatus.CANCELLED ||
            session.getStatus() == LendingSessionStatus.EXPIRED ||
            session.getStatus() == LendingSessionStatus.FAILED) {
            throw new InvalidSessionException("Session is not active. Current status: " + session.getStatus());
        }

        if (session.getStatus() == LendingSessionStatus.STARTED) {
            session.setStatus(LendingSessionStatus.ACTIVE);
            log.info("Transitioned sessionId={} status STARTED -> ACTIVE", sessionId);
        }

        session.setLastActivityAt(OffsetDateTime.now());
        session.setTotalBorrowersEvaluated(session.getTotalBorrowersEvaluated() + 1);
        return sessionRepository.save(session);
    }

    @Transactional
    public void recordInvestmentResult(String sessionId, boolean success, BigDecimal amount) {
        LendingSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InvalidSessionException("Session not found: " + sessionId));

        session.setTotalInvestments(session.getTotalInvestments() + 1);
        if (success) {
            session.setSuccessfulInvestments(session.getSuccessfulInvestments() + 1);
            session.setTotalAmountInvested(session.getTotalAmountInvested().add(amount));
        } else {
            session.setFailedInvestments(session.getFailedInvestments() + 1);
        }
        session.setLastActivityAt(OffsetDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional
    public LendingSessionResponse completeSession(String sessionId) {
        LendingSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        session.setStatus(LendingSessionStatus.COMPLETED);
        session.setCompletedAt(OffsetDateTime.now());
        sessionRepository.save(session);
        log.info("Completed sessionId={}", sessionId);

        return mapToResponse(session);
    }

    @Transactional
    public LendingSessionResponse cancelSession(String sessionId) {
        LendingSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        session.setStatus(LendingSessionStatus.CANCELLED);
        session.setCompletedAt(OffsetDateTime.now());
        sessionRepository.save(session);
        log.info("Cancelled sessionId={}", sessionId);

        return mapToResponse(session);
    }

    public LendingSessionResponse getSessionSummary(String sessionId) {
        LendingSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        return mapToResponse(session);
    }

    private LendingSessionResponse mapToResponse(LendingSession session) {
        return LendingSessionResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus())
                .totalBorrowersEvaluated(session.getTotalBorrowersEvaluated())
                .totalInvestments(session.getTotalInvestments())
                .totalAmountInvested(session.getTotalAmountInvested())
                .build();
    }
}
