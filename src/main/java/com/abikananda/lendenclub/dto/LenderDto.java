package com.abikananda.lendenclub.dto;

import com.abikananda.lendenclub.domain.LendingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LenderDto {
    private String lenderId;
    private String name;
    private BigDecimal walletAmount;
    private String username;
    private String mobileNumber;
    private String otpUsername;
    private List<LendingRule> lendingRules;
    private boolean active;
}
