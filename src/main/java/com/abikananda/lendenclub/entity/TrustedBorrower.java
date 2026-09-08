package com.abikananda.lendenclub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trusted_borrower")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustedBorrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_profile_id", nullable = false, unique = true)
    private BorrowerProfile borrowerProfile;

    @Column(name = "borrower_name", nullable = false, length = 160)
    private String borrowerName;

    @Column(name = "trust_score", precision = 7, scale = 4)
    private BigDecimal trustScore;

    @Column(name = "successful_repayment_count")
    private Long successfulRepaymentCount;

    @Column(name = "total_repaid_amount", precision = 14, scale = 2)
    private BigDecimal totalRepaidAmount;

    @Column(name = "latest_credit_score")
    private Integer latestCreditScore;

    @Column(name = "latest_lenden_score")
    private Integer latestLendenScore;

    @Column(name = "qualification_reason", length = 500)
    private String qualificationReason;

    @Column(name = "source", length = 100)
    private String source;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "derived_at", nullable = false)
    private OffsetDateTime derivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
