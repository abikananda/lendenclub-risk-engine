package com.abikananda.lendenclub.entity;

import com.abikananda.lendenclub.domain.LendingRule;
import com.abikananda.lendenclub.domain.LendingRuleListConverter;
import com.abikananda.lendenclub.domain.LendingSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "lending_session")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LendingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lender_id", nullable = false)
    private Lender lender;

    @Column(name = "configured_investment_amount", precision = 12, scale = 2)
    private BigDecimal configuredInvestmentAmount;

    @Convert(converter = LendingRuleListConverter.class)
    @Column(name = "configured_lending_rules", length = 1000)
    private List<LendingRule> configuredLendingRules;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LendingSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Builder.Default
    @Column(name = "total_borrowers_scanned", nullable = false)
    private Integer totalBorrowersScanned = 0;

    @Builder.Default
    @Column(name = "total_borrowers_evaluated", nullable = false)
    private Integer totalBorrowersEvaluated = 0;

    @Builder.Default
    @Column(name = "total_investments", nullable = false)
    private Integer totalInvestments = 0;

    @Builder.Default
    @Column(name = "total_amount_invested", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountInvested = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "successful_investments", nullable = false)
    private Integer successfulInvestments = 0;

    @Builder.Default
    @Column(name = "failed_investments", nullable = false)
    private Integer failedInvestments = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
