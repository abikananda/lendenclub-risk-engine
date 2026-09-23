package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import com.abikananda.lendenclub.service.BorrowerIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "selenium-borrower-import", name = "enabled", havingValue = "true")
public class SeleniumBorrowerImportUtility {
    private static final Logger log = LoggerFactory.getLogger(SeleniumBorrowerImportUtility.class);
    private static final String MIGRATION_SESSION = "SELENIUM-MIGRATION";

    static final String SOURCE_QUERY = """
            SELECT bl.id, bl.loanId, bl.creditScore, bl.lendenScore, bl.income, bl.loanAmount,
                   bl.borrowerType, bl.interestRate, bl.name, bl.age, bl.lendingAmount,
                   bl.tenure, bl.user, bl.created_date, investment.borrower_gender
            FROM borrower_loans bl
            LEFT JOIN (
                SELECT TRIM(loan_id) AS loan_id,
                       LOWER(TRIM(borrower_name)) AS normalized_borrower_name,
                       CASE
                           WHEN COUNT(DISTINCT UPPER(TRIM(NULLIF(borrower_gender, '-')))) = 1
                           THEN MAX(NULLIF(TRIM(borrower_gender), '-'))
                           ELSE NULL
                       END AS borrower_gender
                FROM invested_loans
                WHERE loan_id IS NOT NULL
                  AND TRIM(loan_id) <> ''
                  AND borrower_name IS NOT NULL
                  AND TRIM(borrower_name) <> ''
                GROUP BY TRIM(loan_id), LOWER(TRIM(borrower_name))
            ) investment
              ON investment.loan_id = TRIM(bl.loanId)
             AND investment.normalized_borrower_name = LOWER(TRIM(bl.name))
            WHERE bl.name IS NOT NULL AND TRIM(bl.name) <> ''
            ORDER BY bl.id
            """;

    private final JdbcTemplate source;
    private final BorrowerIdentityService identities;
    private final BorrowerProfileRepository profiles;
    private final BorrowerSnapshotRepository snapshots;

    public SeleniumBorrowerImportUtility(
            @Qualifier("seleniumSourceJdbcTemplate") JdbcTemplate source,
            BorrowerIdentityService identities,
            BorrowerProfileRepository profiles,
            BorrowerSnapshotRepository snapshots) {
        this.source = source;
        this.identities = identities;
        this.profiles = profiles;
        this.snapshots = snapshots;
    }

    public ImportResult sync() {
        List<SourceBorrower> rows = source.query(SOURCE_QUERY, this::map);
        long profilesBefore = profiles.count();
        Set<Long> matchedProfiles = new HashSet<>();
        Set<Long> encounteredProfiles = new HashSet<>();
        int snapshotsCreated = 0;
        int snapshotsLinked = 0;
        int existingSnapshots = 0;
        int skippedRows = 0;

        for (SourceBorrower row : rows) {
            if (!hasRequiredIdentity(row)) {
                skippedRows++;
                continue;
            }

            String loanId = row.loanId().trim();
            List<BorrowerSnapshot> existing = snapshots.findByLoanId(loanId);
            if (!existing.isEmpty() && existing.stream().allMatch(snapshot -> snapshot.getBorrowerProfile() != null)) {
                existingSnapshots++;
                existing.stream().map(BorrowerSnapshot::getBorrowerProfile)
                        .map(BorrowerProfile::getId).forEach(matchedProfiles::add);
                continue;
            }

            BorrowerProfile profile = identities.resolveOrCreateAt(
                    row.name(), row.gender(), row.borrowerType(), row.age(), row.createdAt());
            matchedProfiles.add(profile.getId());
            boolean repeated = !encounteredProfiles.add(profile.getId());
            if (!existing.isEmpty()) {
                boolean linked = false;
                for (BorrowerSnapshot snapshot : existing) {
                    if (snapshot.getBorrowerProfile() == null) {
                        snapshot.setBorrowerProfile(profile);
                        snapshots.save(snapshot);
                        snapshotsLinked++;
                        linked = true;
                    }
                }
                if (!linked) existingSnapshots++;
                continue;
            }

            snapshots.save(toSnapshot(row, profile, repeated));
            snapshotsCreated++;
        }

        ImportResult result = new ImportResult(rows.size(), matchedProfiles.size(),
                Math.toIntExact(profiles.count() - profilesBefore), snapshotsCreated,
                snapshotsLinked, existingSnapshots, skippedRows);
        log.info("Selenium borrower import completed sourceRows={} uniqueProfiles={} profilesCreated={} snapshotsCreated={} snapshotsLinked={} existingSnapshots={} skippedRows={}",
                result.sourceRows(), result.uniqueProfiles(), result.profilesCreated(), result.snapshotsCreated(),
                result.snapshotsLinked(), result.existingSnapshots(), result.skippedRows());
        return result;
    }

    private boolean hasRequiredIdentity(SourceBorrower row) {
        return row.loanId() != null && !row.loanId().isBlank()
                && present(row.name())
                && present(row.gender())
                && present(row.borrowerType())
                && row.age() != null && row.age() > 0;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank() && !"-".equals(value.trim());
    }

    private BorrowerSnapshot toSnapshot(SourceBorrower row, BorrowerProfile profile, boolean repeated) {
        int tenure = positive(row.tenure(), 1);
        BigDecimal loanAmount = positive(row.loanAmount());
        return BorrowerSnapshot.builder()
                .loanId(row.loanId().trim())
                .borrowerName(row.name().trim())
                .borrowerProfile(profile)
                .creditScore(nonNegativeInt(row.creditScore()))
                .lendenScore(nonNegativeInt(row.lendenScore()))
                .income(nonNegative(row.income()))
                .loanAmount(loanAmount)
                .interestRate(nonNegative(row.interestRate()))
                .tenureMonths(tenure)
                .emi(loanAmount.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP))
                .age(positive(row.age(), 1))
                .borrowerType(defaultText(row.borrowerType(), "UNKNOWN"))
                .gender(row.gender().trim())
                .repeated(repeated)
                .rawPayload("{\"source\":\"selenium.borrower_loans\",\"sourceId\":" + row.sourceId() + "}")
                .scrapedAt(row.createdAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : row.createdAt())
                .sessionId(MIGRATION_SESSION)
                .build();
    }

    private SourceBorrower map(ResultSet rs, int rowNumber) throws SQLException {
        return new SourceBorrower(rs.getLong("id"), rs.getString("loanId"),
                decimal(rs, "creditScore"), decimal(rs, "lendenScore"), decimal(rs, "income"),
                decimal(rs, "loanAmount"), rs.getString("borrowerType"), decimal(rs, "interestRate"),
                rs.getString("name"), integer(rs, "age"), decimal(rs, "lendingAmount"),
                integer(rs, "tenure"), rs.getString("user"),
                rs.getTimestamp("created_date") == null ? null
                        : rs.getTimestamp("created_date").toInstant().atOffset(ZoneOffset.UTC),
                rs.getString("borrower_gender"));
    }

    private BigDecimal decimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer integer(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal positive(BigDecimal value) {
        BigDecimal normalized = nonNegative(value);
        return normalized.signum() == 0 ? BigDecimal.ONE : normalized;
    }

    private int nonNegativeInt(BigDecimal value) {
        return nonNegative(value).intValue();
    }

    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    record SourceBorrower(long sourceId, String loanId, BigDecimal creditScore, BigDecimal lendenScore,
                          BigDecimal income, BigDecimal loanAmount, String borrowerType,
                          BigDecimal interestRate, String name, Integer age, BigDecimal lendingAmount,
                          Integer tenure, String user, OffsetDateTime createdAt, String gender) { }

    public record ImportResult(int sourceRows, int uniqueProfiles, int profilesCreated,
                               int snapshotsCreated, int snapshotsLinked,
                               int existingSnapshots, int skippedRows) { }
}
