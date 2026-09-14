package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.BorrowerLookupResponse;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.exception.ResourceNotFoundException;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BorrowerLookupServiceTest {

    private final BorrowerSnapshotRepository repository = mock(BorrowerSnapshotRepository.class);
    private final BorrowerLookupService service = new BorrowerLookupService(repository);

    @Test
    void returnsBorrowerNameFromLatestSnapshot() {
        BorrowerSnapshot snapshot = BorrowerSnapshot.builder()
                .loanId("LN-1001")
                .borrowerName("  Jane Doe  ")
                .borrowerProfile(BorrowerProfile.builder().publicId("BRW-123").build())
                .build();
        when(repository.findTopByLoanIdAndBorrowerNameIsNotNullOrderByScrapedAtDesc("LN-1001"))
                .thenReturn(Optional.of(snapshot));

        BorrowerLookupResponse response = service.findByLoanId(" LN-1001 ");

        assertEquals("BRW-123", response.borrowerId());
        assertEquals("LN-1001", response.loanId());
        assertEquals("Jane Doe", response.name());
        verify(repository).findTopByLoanIdAndBorrowerNameIsNotNullOrderByScrapedAtDesc("LN-1001");
    }

    @Test
    void throwsNotFoundWhenLoanDoesNotExist() {
        when(repository.findTopByLoanIdAndBorrowerNameIsNotNullOrderByScrapedAtDesc("UNKNOWN"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.findByLoanId("UNKNOWN"));

        assertEquals("Borrower not found for loan ID UNKNOWN", exception.getMessage());
    }

    @Test
    void throwsNotFoundWhenLatestSnapshotHasBlankName() {
        BorrowerSnapshot snapshot = BorrowerSnapshot.builder()
                .loanId("LN-1002")
                .borrowerName("   ")
                .build();
        when(repository.findTopByLoanIdAndBorrowerNameIsNotNullOrderByScrapedAtDesc("LN-1002"))
                .thenReturn(Optional.of(snapshot));

        assertThrows(ResourceNotFoundException.class, () -> service.findByLoanId("LN-1002"));
    }

    @Test
    void throwsNotFoundWhenSnapshotHasNoPersistentBorrowerIdentity() {
        BorrowerSnapshot snapshot = BorrowerSnapshot.builder()
                .loanId("LN-1003")
                .borrowerName("Jane Doe")
                .build();
        when(repository.findTopByLoanIdAndBorrowerNameIsNotNullOrderByScrapedAtDesc("LN-1003"))
                .thenReturn(Optional.of(snapshot));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.findByLoanId("LN-1003"));

        assertEquals("Borrower identity not found for loan ID LN-1003", exception.getMessage());
    }
}
