package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.entity.NpaBorrower;
import com.abikananda.lendenclub.repository.NpaBorrowerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "npa-import", name = "enabled", havingValue = "true")
public class NpaBorrowerImportUtility {

    private static final Logger log = LoggerFactory.getLogger(NpaBorrowerImportUtility.class);

    static final String MANUAL_LENDING_NPA_QUERY = """
            SELECT DISTINCT particular
            FROM manual_lending_investment
            WHERE isnpa = true
              AND particular IS NOT NULL
            """;

    static final String DEFAULT_BORROWERS_QUERY = """
            SELECT DISTINCT name
            FROM default_borrowers
            WHERE name IS NOT NULL
            """;

    private final JdbcTemplate sourceJdbcTemplate;
    private final NpaBorrowerRepository npaBorrowerRepository;

    public NpaBorrowerImportUtility(
            @Qualifier("npaSourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate,
            NpaBorrowerRepository npaBorrowerRepository) {
        this.sourceJdbcTemplate = sourceJdbcTemplate;
        this.npaBorrowerRepository = npaBorrowerRepository;
    }

    @Transactional
    public ImportResult sync() {
        List<String> manualLendingNames = sourceJdbcTemplate.queryForList(MANUAL_LENDING_NPA_QUERY, String.class);
        List<String> defaultBorrowerNames = sourceJdbcTemplate.queryForList(DEFAULT_BORROWERS_QUERY, String.class);

        List<String> sourceNames = new ArrayList<>(manualLendingNames.size() + defaultBorrowerNames.size());
        sourceNames.addAll(manualLendingNames);
        sourceNames.addAll(defaultBorrowerNames);

        Map<String, String> uniqueNames = new LinkedHashMap<>();
        for (String sourceName : sourceNames) {
            if (sourceName == null || sourceName.isBlank()) {
                continue;
            }
            String trimmed = sourceName.trim();
            uniqueNames.putIfAbsent(normalize(trimmed), trimmed);
        }

        int inserted = 0;
        int reactivated = 0;
        int unchanged = 0;

        for (Map.Entry<String, String> entry : uniqueNames.entrySet()) {
            String normalizedName = entry.getKey();
            String borrowerName = entry.getValue();

            var existing = npaBorrowerRepository.findByNormalizedName(normalizedName);
            if (existing.isEmpty()) {
                npaBorrowerRepository.save(NpaBorrower.builder()
                        .borrowerName(borrowerName)
                        .active(true)
                        .build());
                inserted++;
                continue;
            }

            NpaBorrower borrower = existing.get();
            if (!Boolean.TRUE.equals(borrower.getActive())) {
                borrower.setActive(true);
                borrower.setBorrowerName(borrowerName);
                npaBorrowerRepository.save(borrower);
                reactivated++;
            } else {
                unchanged++;
            }
        }

        ImportResult result = new ImportResult(
                manualLendingNames.size(),
                defaultBorrowerNames.size(),
                uniqueNames.size(),
                inserted,
                reactivated,
                unchanged);

        log.info("NPA borrower import completed manualLendingRows={} defaultBorrowerRows={} uniqueNames={} inserted={} reactivated={} unchanged={}",
                result.manualLendingRows(), result.defaultBorrowerRows(), result.uniqueNames(), result.inserted(), result.reactivated(), result.unchanged());
        return result;
    }

    static String normalize(String borrowerName) {
        return borrowerName.trim().toLowerCase(Locale.ROOT);
    }

    public record ImportResult(
            int manualLendingRows,
            int defaultBorrowerRows,
            int uniqueNames,
            int inserted,
            int reactivated,
            int unchanged) {

        public int sourceRows() {
            return manualLendingRows + defaultBorrowerRows;
        }
    }
}
