package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class BorrowerIdentityService {

    private static final Logger log = LoggerFactory.getLogger(BorrowerIdentityService.class);

    private final BorrowerProfileRepository repository;

    public BorrowerIdentityService(BorrowerProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BorrowerProfile resolveOrCreate(String borrowerName, String gender, Integer age) {
        String normalizedName = normalizeName(borrowerName);
        String normalizedGender = normalizeOptional(gender);
        Integer estimatedBirthYear = estimateBirthYear(age);

        List<BorrowerProfile> candidates = repository.findByNormalizedName(normalizedName).stream()
                .filter(profile -> Objects.equals(normalizedGender, profile.getGenderNormalized()))
                .filter(profile -> birthYearCompatible(estimatedBirthYear, profile.getBirthYearEstimate()))
                .toList();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (candidates.size() == 1) {
            BorrowerProfile profile = candidates.get(0);
            profile.setDisplayName(borrowerName.trim());
            profile.setLastSeenAt(now);
            if (profile.getGenderNormalized() == null) profile.setGenderNormalized(normalizedGender);
            if (profile.getBirthYearEstimate() == null) profile.setBirthYearEstimate(estimatedBirthYear);
            return repository.save(profile);
        }

        if (candidates.size() > 1) {
            log.warn("Ambiguous borrower identity name={} gender={} estimatedBirthYear={} candidateCount={}; creating separate profile",
                    borrowerName, normalizedGender, estimatedBirthYear, candidates.size());
        }

        BorrowerProfile created = BorrowerProfile.builder()
                .publicId(UUID.randomUUID().toString())
                .displayName(borrowerName.trim())
                .normalizedName(normalizedName)
                .genderNormalized(normalizedGender)
                .birthYearEstimate(estimatedBirthYear)
                .totalLent(BigDecimal.ZERO)
                .successfulInvestmentCount(0L)
                .lastSeenAt(now)
                .build();
        return repository.save(created);
    }

    @Transactional
    public void recordSuccessfulInvestment(BorrowerProfile profile, BigDecimal amount) {
        if (profile == null || amount == null) return;
        profile.setTotalLent(profile.getTotalLent().add(amount));
        profile.setSuccessfulInvestmentCount(profile.getSuccessfulInvestmentCount() + 1);
        profile.setLastLentAt(OffsetDateTime.now(ZoneOffset.UTC));
        repository.save(profile);
    }

    static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Borrower name is required for identity tracking");
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static Integer estimateBirthYear(Integer age) {
        if (age == null || age <= 0) return null;
        return Year.now(ZoneOffset.UTC).getValue() - age;
    }

    private static boolean birthYearCompatible(Integer observed, Integer stored) {
        if (observed == null || stored == null) return observed == null && stored == null;
        return Math.abs(observed - stored) <= 1;
    }
}
