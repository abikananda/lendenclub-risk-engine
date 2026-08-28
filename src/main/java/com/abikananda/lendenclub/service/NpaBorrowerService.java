package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.NpaBorrowerHitRequest;
import com.abikananda.lendenclub.dto.NpaBorrowerResponse;
import com.abikananda.lendenclub.entity.NpaBorrower;
import com.abikananda.lendenclub.exception.ResourceNotFoundException;
import com.abikananda.lendenclub.repository.NpaBorrowerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NpaBorrowerService {

    private static final Logger log = LoggerFactory.getLogger(NpaBorrowerService.class);

    private final NpaBorrowerRepository repository;
    private final LendingSessionService sessionService;

    public NpaBorrowerService(NpaBorrowerRepository repository, LendingSessionService sessionService) {
        this.repository = repository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public List<NpaBorrowerResponse> getActiveBorrowers() {
        return repository.findAllByActiveTrueOrderByBorrowerNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void recordHit(Long id, NpaBorrowerHitRequest request) {
        sessionService.requireActiveSession(request.getSessionId());

        int updated = repository.recordHit(
                id,
                OffsetDateTime.now(),
                request.getSessionId(),
                request.getLoanId()
        );

        if (updated == 0) {
            throw new ResourceNotFoundException("Active NPA borrower not found: " + id);
        }

        log.warn("sessionId={} loanId={} NPA borrower matched id={}",
                request.getSessionId(), request.getLoanId(), id);
    }

    private NpaBorrowerResponse toResponse(NpaBorrower borrower) {
        return NpaBorrowerResponse.builder()
                .id(borrower.getId())
                .borrowerName(borrower.getBorrowerName())
                .normalizedName(borrower.getNormalizedName())
                .hitCount(borrower.getHitCount())
                .build();
    }
}
