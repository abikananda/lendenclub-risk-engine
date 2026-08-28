package com.abikananda.lendenclub.entity;

import com.abikananda.lendenclub.domain.InvestmentStatus;
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
@Table(name = "lender")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Lender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_lender_id", nullable = false, unique = true, length = 64)
    private String externalLenderId;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "wallet_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal walletAmount;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "mobile_number", nullable = false, unique = true, length = 15)
    private String mobileNumber;

    @Column(name = "otp_username", nullable = false, unique = true, length = 50)
    private String otpUsername;

    @Column(name = "otp_password", nullable = false, length = 100)
    private String otpPassword;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Convert(converter = LendingRuleListConverter.class)
    @Column(name = "lending_rules", length = 1000)
    private List<LendingRule> lendingRules;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
