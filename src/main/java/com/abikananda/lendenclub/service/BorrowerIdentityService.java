package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.exception.ResourceNotFoundException;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BorrowerProfile resolveOrCreate(String borrowerName,
                                           String gender,
                                           String borrowerType,
                                           Integer age) {
        String normalizedName = normalizeName(borrowerName);
        String normalizedGender = normalizeOptional(gender);
        String normalizedBorrowerType = normalizeOptional(borrowerType);
        Integer estimatedBirthYear = estimateBirthYear(age);

        List<BorrowerProfile> candidates = repository.findByNormalizedName(normalizedName).stream()
                .filter(profile -> Objects.equals(normalizedGender, profile.getGenderNormalized()))
                .filter(profile -> Objects.equals(normalizedBorrowerType, profile.getBorrowerTypeNormalized()))
                .filter(profile -> birthYearCompatible(estimatedBirthYear, profile.getBirthYearEstimate()))
                .toList();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (candidates.size() == 1) {
            BorrowerProfile profile = candidates.get(0);
            profile.setDisplayName(borrowerName.trim());
            profile.setLastSeenAt(now);
            if (profile.getGenderNormalized() == null) profile.setGenderNormalized(normalizedGender);
            if (profile.getBorrowerTypeNormalized() == null) profile.setBorrowerTypeNormalized(normalizedBorrowerType);
            if (profile.getBirthYearEstimate() == null) profile.setBirthYearEstimate(estimatedBirthYear);
            return repository.save(profile);
        }

        if (candidates.size() > 1) {
            log.warn("Ambiguous borrower identity name={} gender={} borrowerType={} estimatedBirthYear={} candidateCount={}; creating separate profile",
                    borrowerName, normalizedGender, normalizedBorrowerType, estimatedBirthYear, candidates.size());
        }

        BorrowerProfile created = BorrowerProfile.builder()
                .publicId(UUID.randomUUID().toString())
                .displayName(borrowerName.trim())
                .normalizedName(normalizedName)
                .genderNormalized(normalizedGender)
                .borrowerTypeNormalized(normalizedBorrowerType)
                .birthYearEstimate(estimatedBirthYear)
                .totalLent(BigDecimal.ZERO)
                .successfulInvestmentCount(0L)
                .lastSeenAt(now)
                .build();
        return repository.save(created);
    }

    @Transactional
    public BorrowerProfile recordSuccessfulInvestment(BorrowerProfile profile, BigDecimal amount) {
        if (profile == null || profile.getId() == null || amount == null) return profile;

        BorrowerProfile locked = repository.findByIdForUpdate(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower profile not found: " + profile.getId()));
        locked.setTotalLent(locked.getTotalLent().add(amount));
        locked.setSuccessfulInvestmentCount(locked.getSuccessfulInvestmentCount() + 1);
        locked.setLastLentAt(OffsetDateTime.now(ZoneOffset.UTC));
        return repository.save(locked);
    }

    @Transactional(readOnly = true)
    public BorrowerProfile getByPublicId(String publicId) {
        return repository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower profile not found: " + publicId));
    }

    @Transactional(readOnly = true)
    public List<BorrowerProfile> findByName(String borrowerName) {
        return repository.findByNormalizedName(normalizeName(borrowerName));
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
