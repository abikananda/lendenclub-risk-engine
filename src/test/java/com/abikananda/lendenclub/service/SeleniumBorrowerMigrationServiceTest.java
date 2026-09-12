package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.SeleniumBorrowerRecord;
import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeleniumBorrowerMigrationServiceTest {
    private final SeleniumBorrowerClient client = mock(SeleniumBorrowerClient.class);
    private final BorrowerIdentityService identities = mock(BorrowerIdentityService.class);
    private final BorrowerProfileRepository profiles = mock(BorrowerProfileRepository.class);
    private final BorrowerSnapshotRepository snapshots = mock(BorrowerSnapshotRepository.class);
    private final SeleniumBorrowerMigrationService service =
            new SeleniumBorrowerMigrationService(client, identities, profiles, snapshots);

    @Test
    void fetchesOneBorrowerAtATimeAndCreatesAProfileSnapshot() {
        SeleniumBorrowerRecord row = row();
        BorrowerProfile profile = BorrowerProfile.builder().id(7L).publicId("BRW-ABC").build();
        when(client.ids(0)).thenReturn(new SeleniumBorrowerClient.IdPage(List.of(11L), 11L, false));
        when(client.borrower(11L)).thenReturn(row);
        when(profiles.count()).thenReturn(2L, 3L);
        when(identities.resolveOrCreateAt("Jane Doe", null, "SALARIED", 35,
                OffsetDateTime.parse("2026-01-01T10:00:00Z"))).thenReturn(profile);
        when(snapshots.findByLoanId("LN-1001")).thenReturn(List.of());

        var result = service.migrate(0, 100);

        assertTrue(result.complete());
        assertEquals(11L, result.lastProcessedId());
        assertEquals(1, result.processedRows());
        assertEquals(1, result.profilesCreated());
        assertEquals(1, result.snapshotsCreated());
        ArgumentCaptor<BorrowerSnapshot> captor = ArgumentCaptor.forClass(BorrowerSnapshot.class);
        verify(snapshots).save(captor.capture());
        assertEquals("LN-1001", captor.getValue().getLoanId());
        assertSame(profile, captor.getValue().getBorrowerProfile());
        assertEquals(new BigDecimal("3000.00"), captor.getValue().getEmi());
    }

    @Test
    void reportsFailureAndReturnsCursorSoMigrationCanResume() {
        when(client.ids(20)).thenReturn(new SeleniumBorrowerClient.IdPage(List.of(21L), 21L, true));
        when(client.borrower(21L)).thenThrow(new IllegalStateException("temporary source failure"));
        when(profiles.count()).thenReturn(3L, 3L);

        var result = service.migrate(20, 1);

        assertFalse(result.complete());
        assertEquals(21L, result.lastProcessedId());
        assertEquals(1, result.failedRows());
        assertEquals("temporary source failure", result.failures().get(0).message());
    }

    private SeleniumBorrowerRecord row() {
        return new SeleniumBorrowerRecord(11L, " LN-1001 ", 701, 800,
                new BigDecimal("50000"), new BigDecimal("12000"), "SALARIED",
                new BigDecimal("36.48"), "Jane Doe", 35, new BigDecimal("1000"),
                4, "abikananda", Instant.parse("2026-01-01T10:00:00Z"));
    }
}
