package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;

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
        BorrowerIdentityService service = new BorrowerIdentityService(repository);
        int currentYear = Year.now(ZoneOffset.UTC).getValue();

        BorrowerProfile existing = BorrowerProfile.builder()
                .id(1L)
                .publicId("existing-profile")
                .displayName("Alex Kumar")
                .normalizedName("alex kumar")
                .genderNormalized("male")
                .birthYearEstimate(currentYear - 31)
                .totalLent(new BigDecimal("2000.00"))
                .successfulInvestmentCount(2L)
                .build();

        when(repository.findByNormalizedName("alex kumar")).thenReturn(List.of(existing));
        when(repository.save(any(BorrowerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerProfile resolved = service.resolveOrCreate("  ALEX   KUMAR ", "Male", 30);

        assertEquals("existing-profile", resolved.getPublicId());
        assertEquals(new BigDecimal("2000.00"), resolved.getTotalLent());
        assertEquals(2L, resolved.getSuccessfulInvestmentCount());
    }

    @Test
    void createsSeparateProfileWhenSameNameCandidatesAreAmbiguous() {
        BorrowerProfileRepository repository = mock(BorrowerProfileRepository.class);
        BorrowerIdentityService service = new BorrowerIdentityService(repository);
        int birthYear = Year.now(ZoneOffset.UTC).getValue() - 30;

        BorrowerProfile first = BorrowerProfile.builder()
                .publicId("first")
                .normalizedName("alex kumar")
                .genderNormalized("male")
                .birthYearEstimate(birthYear)
                .build();
        BorrowerProfile second = BorrowerProfile.builder()
                .publicId("second")
                .normalizedName("alex kumar")
                .genderNormalized("male")
                .birthYearEstimate(birthYear + 1)
                .build();

        when(repository.findByNormalizedName("alex kumar")).thenReturn(List.of(first, second));
        when(repository.save(any(BorrowerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerProfile resolved = service.resolveOrCreate("Alex Kumar", "Male", 30);

        assertNotNull(resolved.getPublicId());
        assertNotSame(first, resolved);
        assertNotSame(second, resolved);
        assertEquals(BigDecimal.ZERO, resolved.getTotalLent());
        assertEquals(0L, resolved.getSuccessfulInvestmentCount());
    }
}
