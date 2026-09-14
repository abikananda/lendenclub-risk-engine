package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import com.abikananda.lendenclub.service.BorrowerIdentityService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeleniumBorrowerImportUtilityTest {
    private final JdbcTemplate source = mock(JdbcTemplate.class);
    private final BorrowerIdentityService identities = mock(BorrowerIdentityService.class);
    private final BorrowerProfileRepository profiles = mock(BorrowerProfileRepository.class);
    private final BorrowerSnapshotRepository snapshots = mock(BorrowerSnapshotRepository.class);
    private final SeleniumBorrowerImportUtility utility =
            new SeleniumBorrowerImportUtility(source, identities, profiles, snapshots);

    @Test
    void createsProfileAndHistoricalSnapshotFromSeleniumBorrower() {
        var row = sourceRow();
        BorrowerProfile profile = BorrowerProfile.builder().id(10L).publicId("BRW-ABC").build();
        when(source.query(eq(SeleniumBorrowerImportUtility.SOURCE_QUERY),
                org.mockito.ArgumentMatchers.<RowMapper<SeleniumBorrowerImportUtility.SourceBorrower>>any()))
                .thenReturn(List.of(row));
        when(profiles.count()).thenReturn(2L, 3L);
        when(identities.resolveOrCreateAt("Jane Doe", null, "SALARIED", 35, row.createdAt())).thenReturn(profile);
        when(snapshots.findByLoanId("LN-1001")).thenReturn(List.of());

        var result = utility.sync();

        assertEquals(1, result.sourceRows());
        assertEquals(1, result.uniqueProfiles());
        assertEquals(1, result.profilesCreated());
        assertEquals(1, result.snapshotsCreated());
        ArgumentCaptor<BorrowerSnapshot> captor = ArgumentCaptor.forClass(BorrowerSnapshot.class);
        verify(snapshots).save(captor.capture());
        BorrowerSnapshot saved = captor.getValue();
        assertEquals("LN-1001", saved.getLoanId());
        assertEquals("Jane Doe", saved.getBorrowerName());
        assertSame(profile, saved.getBorrowerProfile());
        assertEquals(new BigDecimal("12000"), saved.getLoanAmount());
        assertEquals(new BigDecimal("3000.00"), saved.getEmi());
        assertEquals("SELENIUM-MIGRATION", saved.getSessionId());
    }

    @Test
    void rerunLinksAnExistingUnassignedSnapshotInsteadOfDuplicatingIt() {
        BorrowerProfile profile = BorrowerProfile.builder().id(10L).publicId("BRW-ABC").build();
        BorrowerSnapshot existing = BorrowerSnapshot.builder().loanId("LN-1001").build();
        when(source.query(eq(SeleniumBorrowerImportUtility.SOURCE_QUERY),
                org.mockito.ArgumentMatchers.<RowMapper<SeleniumBorrowerImportUtility.SourceBorrower>>any()))
                .thenReturn(List.of(sourceRow()));
        when(profiles.count()).thenReturn(3L, 3L);
        when(identities.resolveOrCreateAt(any(), eq(null), any(), any(), any())).thenReturn(profile);
        when(snapshots.findByLoanId("LN-1001")).thenReturn(List.of(existing));

        var result = utility.sync();

        assertEquals(0, result.snapshotsCreated());
        assertEquals(1, result.snapshotsLinked());
        assertSame(profile, existing.getBorrowerProfile());
        verify(snapshots).save(existing);
    }

    private SeleniumBorrowerImportUtility.SourceBorrower sourceRow() {
        return new SeleniumBorrowerImportUtility.SourceBorrower(1L, " LN-1001 ",
                new BigDecimal("701"), new BigDecimal("800"), new BigDecimal("50000"),
                new BigDecimal("12000"), "SALARIED", new BigDecimal("36.48"),
                "Jane Doe", 35, new BigDecimal("1000"), 4, "9090000000",
                OffsetDateTime.parse("2026-01-01T10:00:00Z"));
    }
}
