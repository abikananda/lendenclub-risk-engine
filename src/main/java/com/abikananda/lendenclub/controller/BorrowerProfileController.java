package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.dto.BorrowerProfileResponse;
import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.service.BorrowerIdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/borrower-profiles")
public class BorrowerProfileController {

    private final BorrowerIdentityService borrowerIdentityService;

    public BorrowerProfileController(BorrowerIdentityService borrowerIdentityService) {
        this.borrowerIdentityService = borrowerIdentityService;
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<BorrowerProfileResponse> getByPublicId(@PathVariable String publicId) {
        return ResponseEntity.ok(toResponse(borrowerIdentityService.getByPublicId(publicId)));
    }

    @GetMapping
    public ResponseEntity<List<BorrowerProfileResponse>> findByName(@RequestParam String name) {
        return ResponseEntity.ok(
                borrowerIdentityService.findByName(name).stream()
                        .map(this::toResponse)
                        .toList());
    }

    private BorrowerProfileResponse toResponse(BorrowerProfile profile) {
        return BorrowerProfileResponse.builder()
                .borrowerProfileId(profile.getPublicId())
                .displayName(profile.getDisplayName())
                .gender(profile.getGenderNormalized())
                .borrowerType(profile.getBorrowerTypeNormalized())
                .birthYearEstimate(profile.getBirthYearEstimate())
                .totalLent(profile.getTotalLent())
                .successfulInvestmentCount(profile.getSuccessfulInvestmentCount())
                .firstSeenAt(profile.getFirstSeenAt())
                .lastSeenAt(profile.getLastSeenAt())
                .lastLentAt(profile.getLastLentAt())
                .build();
    }
}
