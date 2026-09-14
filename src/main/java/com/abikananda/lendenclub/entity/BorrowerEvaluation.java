package com.abikananda.lendenclub.entity;

import com.abikananda.lendenclub.domain.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "borrower_evaluation")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BorrowerEvaluation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "loan_id", nullable = false, length = 64)
    private String loanId;
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING) @Column(name = "drools_decision", length = 20)
    private LendingDecision droolsDecision;
    @Enumerated(EnumType.STRING) @Column(name = "drools_risk_level", length = 20)
    private RiskLevel droolsRiskLevel;
    @Column(name = "drools_investment_amount", precision = 12, scale = 2)
    private BigDecimal droolsInvestmentAmount;

    @Enumerated(EnumType.STRING) @Column(name = "decision", nullable = false, length = 20)
    private LendingDecision decision;
    @Enumerated(EnumType.STRING) @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;
    @Column(name = "investment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal investmentAmount;
    @Column(name = "rule_name", length = 100)
    private String ruleName;
    @Column(name = "rule_code", length = 50)
    private String ruleCode;
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ai_risk_score")
    private Double aiRiskScore;
    @Enumerated(EnumType.STRING) @Column(name = "ai_risk_level", length = 20)
    private RiskLevel aiRiskLevel;
    @Enumerated(EnumType.STRING) @Column(name = "ai_recommendation", length = 20)
    private AiRecommendation aiRecommendation;
    @Column(name = "ai_confidence")
    private Double aiConfidence;
    @Column(name = "ai_maximum_amount", precision = 12, scale = 2)
    private BigDecimal aiMaximumAmount;
    @Column(name = "ai_rationale", columnDefinition = "TEXT")
    private String aiRationale;
    @Column(name = "ai_concerns", columnDefinition = "JSON")
    private String aiConcerns;
    @Column(name = "ai_positive_factors", columnDefinition = "JSON")
    private String aiPositiveFactors;
    @Column(name = "ai_provider", length = 30)
    private String aiProvider;
    @Column(name = "ai_model", length = 100)
    private String aiModel;
    @Column(name = "ai_prompt_version", length = 30)
    private String aiPromptVersion;
    @Enumerated(EnumType.STRING) @Column(name = "ai_status", length = 30)
    private AiRiskStatus aiStatus;
    @Column(name = "ai_latency_ms")
    private Long aiLatencyMs;

    @Column(name = "engine_version", length = 20)
    private String engineVersion;
    @CreationTimestamp
    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private OffsetDateTime evaluatedAt;
}
