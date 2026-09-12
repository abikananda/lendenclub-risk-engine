package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.SeleniumBorrowerRecord;
import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SeleniumBorrowerMigrationService {
    private static final String MIGRATION_SESSION = "SELENIUM-API-MIGRATION";
    private final SeleniumBorrowerClient client;
    private final BorrowerIdentityService identities;
    private final BorrowerProfileRepository profiles;
    private final BorrowerSnapshotRepository snapshots;

    public SeleniumBorrowerMigrationService(SeleniumBorrowerClient client,
                                             BorrowerIdentityService identities,
                                             BorrowerProfileRepository profiles,
                                             BorrowerSnapshotRepository snapshots) {
        this.client = client;
        this.identities = identities;
        this.profiles = profiles;
        this.snapshots = snapshots;
    }

    public ImportResult migrate(long afterId, int maxRecords) {
        if (afterId < 0) throw new IllegalArgumentException("afterId cannot be negative");
        if (maxRecords < 1 || maxRecords > 10_000)
            throw new IllegalArgumentException("maxRecords must be between 1 and 10000");

        long profilesBefore = profiles.count();
        long cursor = afterId;
        int processed = 0;
        int snapshotsCreated = 0;
        int snapshotsLinked = 0;
        int existingSnapshots = 0;
        Set<Long> uniqueProfiles = new HashSet<>();
        List<ImportFailure> failures = new ArrayList<>();
        boolean complete = false;

        while (processed < maxRecords) {
            SeleniumBorrowerClient.IdPage page = client.ids(cursor);
            if (page == null || page.ids() == null || page.ids().isEmpty()) {
                complete = true;
                break;
            }

            for (Long sourceId : page.ids()) {
                if (processed >= maxRecords) break;
                cursor = sourceId;
                processed++;
                try {
                    SeleniumBorrowerRecord row = client.borrower(sourceId);
                    validate(row);
                    OffsetDateTime observedAt = row.createdDate() == null
                            ? OffsetDateTime.now(ZoneOffset.UTC)
                            : row.createdDate().atOffset(ZoneOffset.UTC);
                    BorrowerProfile profile = identities.resolveOrCreateAt(
                            row.name(), null, row.borrowerType(), row.age(), observedAt);
                    boolean repeated = !uniqueProfiles.add(profile.getId());

                    List<BorrowerSnapshot> existing = snapshots.findByLoanId(row.loanId().trim());
                    if (!existing.isEmpty()) {
                        int linked = linkUnassigned(existing, profile);
                        snapshotsLinked += linked;
                        if (linked == 0) existingSnapshots++;
                    } else {
                        snapshots.save(toSnapshot(row, profile, observedAt, repeated));
                        snapshotsCreated++;
                    }
                } catch (RuntimeException ex) {
                    failures.add(new ImportFailure(sourceId, safeMessage(ex)));
                }
            }

            if (processed >= maxRecords) break;
            if (!page.hasMore()) {
                complete = true;
                break;
            }
            if (page.nextCursor() < cursor) {
                failures.add(new ImportFailure(cursor, "Source cursor did not advance"));
                break;
            }
            cursor = page.nextCursor();
        }

        return new ImportResult(afterId, cursor, complete, processed, uniqueProfiles.size(),
                Math.toIntExact(profiles.count() - profilesBefore), snapshotsCreated,
                snapshotsLinked, existingSnapshots, failures.size(), failures);
    }

    private int linkUnassigned(List<BorrowerSnapshot> existing, BorrowerProfile profile) {
        int linked = 0;
        for (BorrowerSnapshot snapshot : existing) {
            if (snapshot.getBorrowerProfile() == null) {
                snapshot.setBorrowerProfile(profile);
                snapshots.save(snapshot);
                linked++;
            }
        }
        return linked;
    }

    private BorrowerSnapshot toSnapshot(SeleniumBorrowerRecord row, BorrowerProfile profile,
                                        OffsetDateTime observedAt, boolean repeated) {
        int tenure = positive(row.tenure(), 1);
        BigDecimal loanAmount = positive(row.loanAmount());
        return BorrowerSnapshot.builder()
                .loanId(row.loanId().trim()).borrowerName(row.name().trim()).borrowerProfile(profile)
                .creditScore(nonNegative(row.creditScore())).lendenScore(nonNegative(row.lendenScore()))
                .income(nonNegative(row.income())).loanAmount(loanAmount)
                .interestRate(nonNegative(row.interestRate())).tenureMonths(tenure)
                .emi(loanAmount.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP))
                .age(positive(row.age(), 1)).borrowerType(defaultText(row.borrowerType(), "UNKNOWN"))
                .repeated(repeated).scrapedAt(observedAt).sessionId(MIGRATION_SESSION)
                .rawPayload("{\"source\":\"selenium-api\",\"sourceId\":" + row.id() + "}")
                .build();
    }

    private void validate(SeleniumBorrowerRecord row) {
        if (row == null) throw new IllegalArgumentException("Source returned an empty borrower");
        if (row.loanId() == null || row.loanId().isBlank())
            throw new IllegalArgumentException("Source borrower has no loan ID");
        if (row.name() == null || row.name().isBlank())
            throw new IllegalArgumentException("Source borrower has no name");
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private int nonNegative(Integer value) { return value == null || value < 0 ? 0 : value; }
    private BigDecimal positive(BigDecimal value) {
        BigDecimal normalized = nonNegative(value);
        return normalized.signum() == 0 ? BigDecimal.ONE : normalized;
    }
    private int positive(Integer value, int fallback) { return value == null || value <= 0 ? fallback : value; }
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    public record ImportFailure(long sourceId, String message) { }
    public record ImportResult(long requestedAfterId, long lastProcessedId, boolean complete,
                               int processedRows, int uniqueProfiles, int profilesCreated,
                               int snapshotsCreated, int snapshotsLinked, int existingSnapshots,
                               int failedRows, List<ImportFailure> failures) { }
}
