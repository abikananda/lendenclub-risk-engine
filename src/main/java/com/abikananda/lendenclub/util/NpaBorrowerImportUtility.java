package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.config.NpaBorrowerImportProperties;
import com.abikananda.lendenclub.entity.NpaBorrower;
import com.abikananda.lendenclub.repository.NpaBorrowerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "npa-import", name = "enabled", havingValue = "true")
public class NpaBorrowerImportUtility {

    private static final Logger log = LoggerFactory.getLogger(NpaBorrowerImportUtility.class);

    private final JdbcTemplate sourceJdbcTemplate;
    private final NpaBorrowerRepository npaBorrowerRepository;
    private final NpaBorrowerImportProperties properties;

    public NpaBorrowerImportUtility(
            @Qualifier("npaSourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate,
            NpaBorrowerRepository npaBorrowerRepository,
            NpaBorrowerImportProperties properties) {
        this.sourceJdbcTemplate = sourceJdbcTemplate;
        this.npaBorrowerRepository = npaBorrowerRepository;
        this.properties = properties;
    }

    @Transactional
    public ImportResult sync() {
        String query = validateQuery(properties.getQuery());
        List<String> sourceNames = sourceJdbcTemplate.queryForList(query, String.class);

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

        ImportResult result = new ImportResult(sourceNames.size(), uniqueNames.size(), inserted, reactivated, unchanged);
        log.info("NPA borrower import completed sourceRows={} uniqueNames={} inserted={} reactivated={} unchanged={}",
                result.sourceRows(), result.uniqueNames(), result.inserted(), result.reactivated(), result.unchanged());
        return result;
    }

    static String normalize(String borrowerName) {
        return borrowerName.trim().toLowerCase(Locale.ROOT);
    }

    private static String validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalStateException("npa-import.query must be configured when NPA import is enabled");
        }

        String trimmed = query.stripLeading();
        if (!trimmed.regionMatches(true, 0, "select", 0, "select".length())) {
            throw new IllegalArgumentException("npa-import.query must be a read-only SELECT statement");
        }
        if (trimmed.contains(";")) {
            throw new IllegalArgumentException("npa-import.query must contain a single SELECT statement without semicolons");
        }
        return query;
    }

    public record ImportResult(
            int sourceRows,
            int uniqueNames,
            int inserted,
            int reactivated,
            int unchanged) {
    }
}
