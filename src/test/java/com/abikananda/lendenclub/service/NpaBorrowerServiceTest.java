package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.NpaBorrowerHitRequest;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.entity.NpaBorrower;
import com.abikananda.lendenclub.exception.ResourceNotFoundException;
import com.abikananda.lendenclub.repository.NpaBorrowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NpaBorrowerServiceTest {

    @Mock
    private NpaBorrowerRepository repository;

    @Mock
    private LendingSessionService sessionService;

    private NpaBorrowerService service;

    @BeforeEach
    void setUp() {
        service = new NpaBorrowerService(repository, sessionService);
    }

    @Test
    void returnsOnlyActiveBorrowersFromRepository() {
        when(repository.findAllByActiveTrueOrderByBorrowerNameAsc()).thenReturn(List.of(
                NpaBorrower.builder()
                        .id(7L)
                        .borrowerName("NPA Borrower")
                        .normalizedName("npa borrower")
                        .active(true)
                        .hitCount(3L)
                        .build()
        ));

        var result = service.getActiveBorrowers();

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getId());
        assertEquals("NPA Borrower", result.get(0).getBorrowerName());
        assertEquals("npa borrower", result.get(0).getNormalizedName());
        assertEquals(3L, result.get(0).getHitCount());
    }

    @Test
    void recordsHitWithoutIncrementingSessionEvaluationCounter() {
        var request = NpaBorrowerHitRequest.builder()
                .sessionId("LS-TEST")
                .loanId("LOA-TEST")
                .build();

        when(sessionService.requireActiveSession("LS-TEST")).thenReturn(mock(LendingSession.class));
        when(repository.recordHit(eq(7L), any(OffsetDateTime.class), eq("LS-TEST"), eq("LOA-TEST")))
                .thenReturn(1);

        service.recordHit(7L, request);

        verify(sessionService).requireActiveSession("LS-TEST");
        verify(sessionService, never()).validateAndTouchSession(anyString());
        verify(repository).recordHit(eq(7L), any(OffsetDateTime.class), eq("LS-TEST"), eq("LOA-TEST"));
    }

    @Test
    void rejectsMissingOrInactiveNpaBorrower() {
        var request = NpaBorrowerHitRequest.builder()
                .sessionId("LS-TEST")
                .loanId("LOA-TEST")
                .build();

        when(sessionService.requireActiveSession("LS-TEST")).thenReturn(mock(LendingSession.class));
        when(repository.recordHit(eq(99L), any(OffsetDateTime.class), eq("LS-TEST"), eq("LOA-TEST")))
                .thenReturn(0);

        assertThrows(ResourceNotFoundException.class, () -> service.recordHit(99L, request));
    }
}
