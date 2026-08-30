package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.entity.NpaBorrower;
import com.abikananda.lendenclub.repository.NpaBorrowerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NpaBorrowerImportUtilityTest {

    @Test
    void syncCombinesSeleniumSourcesDeduplicatesAndReactivatesWithoutResettingHistory() {
        JdbcTemplate source = mock(JdbcTemplate.class);
        NpaBorrowerRepository repository = mock(NpaBorrowerRepository.class);

        when(source.queryForList(NpaBorrowerImportUtility.MANUAL_LENDING_NPA_QUERY, String.class, true))
                .thenReturn(List.of(" Alice ", "BOB"));
        when(source.queryForList(NpaBorrowerImportUtility.DEFAULT_BORROWERS_QUERY, String.class))
                .thenReturn(List.of("alice", "  "));
        when(repository.findByNormalizedName("alice")).thenReturn(Optional.empty());

        NpaBorrower bob = NpaBorrower.builder()
                .id(7L)
                .borrowerName("Bob")
                .active(false)
                .hitCount(12L)
                .build();
        when(repository.findByNormalizedName("bob")).thenReturn(Optional.of(bob));

        NpaBorrowerImportUtility utility = new NpaBorrowerImportUtility(source, repository);
        var result = utility.sync();

        assertEquals(2, result.manualLendingRows());
        assertEquals(2, result.defaultBorrowerRows());
        assertEquals(4, result.sourceRows());
        assertEquals(2, result.uniqueNames());
        assertEquals(1, result.inserted());
        assertEquals(1, result.reactivated());
        assertEquals(0, result.unchanged());
        assertEquals(12L, bob.getHitCount());
        assertEquals(true, bob.getActive());
        verify(repository).save(bob);
        verify(repository, times(2)).save(any(NpaBorrower.class));
        verify(source).queryForList(NpaBorrowerImportUtility.MANUAL_LENDING_NPA_QUERY, String.class, true);
        verify(source).queryForList(NpaBorrowerImportUtility.DEFAULT_BORROWERS_QUERY, String.class);
    }
}
