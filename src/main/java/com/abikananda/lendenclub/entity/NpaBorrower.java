package com.abikananda.lendenclub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "npa_borrower")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NpaBorrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrower_name", nullable = false, length = 160)
    private String borrowerName;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 160)
    private String normalizedName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private Long hitCount = 0L;

    @Column(name = "last_hit_at")
    private OffsetDateTime lastHitAt;

    @Column(name = "last_session_id", length = 64)
    private String lastSessionId;

    @Column(name = "last_loan_id", length = 64)
    private String lastLoanId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
