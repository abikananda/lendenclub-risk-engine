package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.LendingSessionStatus;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.repository.LendingSessionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LendingSessionServiceTest {

    @Test
    void lockedSessionRetainsOwningLenderAndUpdatesCounters() {
        LendingSessionRepository repository = mock(LendingSessionRepository.class);
        LendingSessionService service = new LendingSessionService(repository);
        Lender lender = Lender.builder().id(10L).externalLenderId("LENDER-10").build();
        LendingSession session = LendingSession.builder()
                .id(20L)
                .sessionId("LS-TEST")
                .lender(lender)
                .status(LendingSessionStatus.ACTIVE)
                .startedAt(OffsetDateTime.now())
                .lastActivityAt(OffsetDateTime.now())
                .totalBorrowersEvaluated(0)
                .totalInvestments(0)
                .successfulInvestments(0)
                .failedInvestments(0)
                .totalAmountInvested(BigDecimal.ZERO)
                .build();

        when(repository.findBySessionIdForUpdate("LS-TEST")).thenReturn(Optional.of(session));
        when(repository.save(session)).thenReturn(session);

        LendingSession locked = service.requireActiveSessionForUpdate("LS-TEST");
        assertSame(lender, locked.getLender());

        service.recordInvestmentResult(locked, true, new BigDecimal("250.00"));

        assertEquals(1, locked.getTotalInvestments());
        assertEquals(1, locked.getSuccessfulInvestments());
        assertEquals(0, new BigDecimal("250.00").compareTo(locked.getTotalAmountInvested()));
        verify(repository).save(locked);
    }
}
