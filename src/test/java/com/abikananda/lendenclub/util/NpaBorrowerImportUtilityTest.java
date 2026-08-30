package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.config.NpaBorrowerImportProperties;
import com.abikananda.lendenclub.entity.NpaBorrower;
import com.abikananda.lendenclub.repository.NpaBorrowerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NpaBorrowerImportUtilityTest {

    @Test
    void syncDeduplicatesInsertsAndReactivatesWithoutResettingHistory() {
        JdbcTemplate source = mock(JdbcTemplate.class);
        NpaBorrowerRepository repository = mock(NpaBorrowerRepository.class);
        NpaBorrowerImportProperties properties = new NpaBorrowerImportProperties();
        properties.setQuery("SELECT borrower_name FROM source_npa WHERE overdue_days >= 90");

        when(source.queryForList(properties.getQuery(), String.class))
                .thenReturn(List.of(" Alice ", "alice", "BOB", "  "));
        when(repository.findByNormalizedName("alice")).thenReturn(Optional.empty());

        NpaBorrower bob = NpaBorrower.builder()
                .id(7L)
                .borrowerName("Bob")
                .active(false)
                .hitCount(12L)
                .build();
        when(repository.findByNormalizedName("bob")).thenReturn(Optional.of(bob));

        NpaBorrowerImportUtility utility = new NpaBorrowerImportUtility(source, repository, properties);
        var result = utility.sync();

        assertEquals(4, result.sourceRows());
        assertEquals(2, result.uniqueNames());
        assertEquals(1, result.inserted());
        assertEquals(1, result.reactivated());
        assertEquals(0, result.unchanged());
        assertEquals(12L, bob.getHitCount());
        assertEquals(true, bob.getActive());
        verify(repository).save(bob);
        verify(repository).save(any(NpaBorrower.class));
    }

    @Test
    void syncRejectsNonSelectQueryBeforeReadingSource() {
        JdbcTemplate source = mock(JdbcTemplate.class);
        NpaBorrowerRepository repository = mock(NpaBorrowerRepository.class);
        NpaBorrowerImportProperties properties = new NpaBorrowerImportProperties();
        properties.setQuery("DELETE FROM source_npa");

        NpaBorrowerImportUtility utility = new NpaBorrowerImportUtility(source, repository, properties);

        assertThrows(IllegalArgumentException.class, utility::sync);
        verify(source, never()).queryForList(any(String.class), any(Class.class));
    }
}
