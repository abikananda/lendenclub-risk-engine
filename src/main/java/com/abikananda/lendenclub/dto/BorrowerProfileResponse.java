package com.abikananda.lendenclub.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class BorrowerProfileResponse {
    private String borrowerProfileId;
    private String displayName;
    private String gender;
    private Integer birthYearEstimate;
    private BigDecimal totalLent;
    private Long successfulInvestmentCount;
    private OffsetDateTime firstSeenAt;
    private OffsetDateTime lastSeenAt;
    private OffsetDateTime lastLentAt;
}
