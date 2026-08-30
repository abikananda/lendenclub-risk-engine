package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowerIdentityServiceTest {

    @Test
    void reusesSingleCompatibleProfileAcrossBirthdayBoundary() {
        BorrowerProfileRepository repository = mock(BorrowerProfileRepository.class);
        BorrowerIdentityService service = service(repository);
        int currentYear = Year.now(ZoneOffset.UTC).getValue();

        BorrowerProfile existing = BorrowerProfile.builder()
                .id(1L)
                .publicId("existing-profile")
                .displayName("Alex Kumar")
                .normalizedName("alex kumar")
                .genderNormalized("male")
                .borrowerTypeNormalized("salaried")
                .birthYearEstimate(currentYear - 31)
                .totalLent(new BigDecimal("2000.00"))
                .successfulInvestmentCount(2L)
                .build();

        when(repository.findByNormalizedName("alex kumar")).thenReturn(List.of(existing));
        when(repository.save(any(BorrowerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerProfile resolved = service.resolveOrCreate("  ALEX   KUMAR ", "Male", "SALARIED", 30);

        assertEquals("existing-profile", resolved.getPublicId());
        assertEquals(new BigDecimal("2000.00"), resolved.getTotalLent());
        assertEquals(2L, resolved.getSuccessfulInvestmentCount());
    }

    @Test
    void doesNotMergeSameNameWhenBorrowerTypeDiffers() {
        BorrowerProfileRepository repository = mock(BorrowerProfileRepository.class);
        BorrowerIdentityService service = service(repository);
        int birthYear = Year.now(ZoneOffset.UTC).getValue() - 30;

        BorrowerProfile existing = BorrowerProfile.builder()
                .publicId("existing")
                .normalizedName("alex kumar")
                .genderNormalized("male")
                .borrowerTypeNormalized("salaried")
                .birthYearEstimate(birthYear)
                .build();

        when(repository.findByNormalizedName("alex kumar")).thenReturn(List.of(existing));

        BorrowerProfile resolved = service.resolveOrCreate("Alex Kumar", "Male", "SELF_EMPLOYED", 30);

        assertNotSame(existing, resolved);
        assertEquals("BRW-23456789AB", resolved.getPublicId());
        assertEquals("self_employed", resolved.getBorrowerTypeNormalized());
    }

    @Test
    void createsSeparateProfileWhenSameNameCandidatesAreAmbiguous() {
        BorrowerProfileRepository repository = mock(BorrowerProfileRepository.class);
        BorrowerIdentityService service = service(repository);
        int birthYear = Year.now(ZoneOffset.UTC).getValue() - 30;

        BorrowerProfile first = BorrowerProfile.builder()
                .publicId("first")
                .normalizedName("alex kumar")
                .genderNormalized("male")
                .borrowerTypeNormalized("salaried")
                .birthYearEstimate(birthYear)
                .build();
        BorrowerProfile second = BorrowerProfile.builder()
                .publicId("second")
                .normalizedName("alex kumar")
                .genderNormalized("male")
                .borrowerTypeNormalized("salaried")
                .birthYearEstimate(birthYear + 1)
                .build();

        when(repository.findByNormalizedName("alex kumar")).thenReturn(List.of(first, second));

        BorrowerProfile resolved = service.resolveOrCreate("Alex Kumar", "Male", "SALARIED", 30);

        assertNotNull(resolved.getPublicId());
        assertNotSame(first, resolved);
        assertNotSame(second, resolved);
        assertEquals(BigDecimal.ZERO, resolved.getTotalLent());
        assertEquals(0L, resolved.getSuccessfulInvestmentCount());
    }

    @SuppressWarnings("unchecked")
    private static BorrowerIdentityService service(BorrowerProfileRepository repository) {
        BorrowerProfileCreator creator = mock(BorrowerProfileCreator.class);
        when(creator.create(any())).thenAnswer(invocation -> {
            Function<String, BorrowerProfile> factory = invocation.getArgument(0);
            return factory.apply("BRW-23456789AB");
        });
        return new BorrowerIdentityService(repository, creator);
    }
}
