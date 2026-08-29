package com.abikananda.lendenclub.entity;

import com.abikananda.lendenclub.domain.LendingRule;
import com.abikananda.lendenclub.domain.LendingRuleListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "investment_config")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InvestmentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lender_id", nullable = false, unique = true)
    private Lender lender;

    @Column(name = "investment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal investmentAmount;

    @Convert(converter = LendingRuleListConverter.class)
    @Column(name = "lending_rules", nullable = false, length = 1000)
    private List<LendingRule> lendingRules;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
