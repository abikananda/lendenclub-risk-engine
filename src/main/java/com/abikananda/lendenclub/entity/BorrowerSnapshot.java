package com.abikananda.lendenclub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "borrower_snapshot")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BorrowerSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false, length = 64)
    private String loanId;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Column(name = "lenden_score", nullable = false)
    private Integer lendenScore;

    @Column(name = "income", nullable = false, precision = 12, scale = 2)
    private BigDecimal income;

    @Column(name = "loan_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emi", nullable = false, precision = 12, scale = 2)
    private BigDecimal emi;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "borrower_type", nullable = false, length = 50)
    private String borrowerType;

    @Column(name = "repeated", nullable = false)
    private Boolean repeated;

    @Column(name = "raw_payload", columnDefinition = "JSON")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "scraped_at", nullable = false, updatable = false)
    private OffsetDateTime scrapedAt;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;
}
