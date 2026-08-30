package com.abikananda.lendenclub.entity;

import com.abikananda.lendenclub.domain.LendingDecision;
import com.abikananda.lendenclub.domain.RiskLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "borrower_evaluation")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BorrowerEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false, length = 64)
    private String loanId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private LendingDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "investment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal investmentAmount;

    @Column(name = "rule_name", length = 100)
    private String ruleName;

    @Column(name = "rule_code", length = 50)
    private String ruleCode;

    @Column(name = "rule_version", length = 30)
    private String ruleVersion;

    @Column(name = "ruleset_version", length = 50)
    private String rulesetVersion;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ai_risk_score")
    private Double aiRiskScore;

    @Column(name = "engine_version", length = 20)
    private String engineVersion;

    @CreationTimestamp
    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private OffsetDateTime evaluatedAt;
}
