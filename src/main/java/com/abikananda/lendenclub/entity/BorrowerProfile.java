package com.abikananda.lendenclub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "borrower_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "normalized_name", nullable = false, length = 160)
    private String normalizedName;

    @Column(name = "gender_normalized", length = 30)
    private String genderNormalized;

    @Column(name = "birth_year_estimate")
    private Integer birthYearEstimate;

    @Column(name = "total_lent", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalLent = BigDecimal.ZERO;

    @Column(name = "successful_investment_count", nullable = false)
    @Builder.Default
    private Long successfulInvestmentCount = 0L;

    @CreationTimestamp
    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private OffsetDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "last_lent_at")
    private OffsetDateTime lastLentAt;
}
