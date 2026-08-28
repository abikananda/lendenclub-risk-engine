package com.abikananda.lendenclub.domain;

import lombok.Getter;

/**
 * Enum representing lending rules. The enum constant name is used as the rule code
 * (via name()), and a human-readable rule name is stored as the value.
 */
@Getter
public enum LendingRule {
    REPEATED_LENDERS_LOW_RISK("Repeated Lenders - Low Risk"),
    REPEATED_LENDERS_MEDIUM_RISK("Repeated Lenders - Medium Risk"),
    REPEATED_LENDERS_HIGH_RISK("Repeated Lenders - High Risk"),
    TRUSTED_LENDERS_LOW_RISK("Trusted Lenders - Low Risk"),
    TRUSTED_LENDERS_MEDIUM_RISK("Trusted Lenders - Medium Risk"),
    TRUSTED_LENDERS_HIGH_RISK("Trusted Lenders - High Risk"),
    GOOD_LENDERS("Good Lenders"),
    NORMAL_LENDERS("Normal Lenders"),
    BULK_LENDERS("Bulk Lenders"),
    REPEATED_BUSINESS_LENDERS("Repeated Business Lenders"),
    GOOD_BUSINESS_LENDERS("Good Business Lenders"),
    BULK_BUSINESS_LENDERS("Bulk Business Lenders"),
    FILLING_FAST_LENDERS("Filling Fast Lenders"),
    DAILY_REPAYMENT_LENDERS("Daily Repayment Lenders"),
    MONTHLY_REPAYMENT_HIGH_RISK("Monthly Repayment - High Risk");

    private final String ruleName;

    LendingRule(String ruleName) {
        this.ruleName = ruleName;
    }

    public static String fromCode(String ruleCode) {
        for (LendingRule rule : LendingRule.values()) {
            if (rule.name().equalsIgnoreCase(ruleCode)) {
                return rule.getRuleName();
            }
        }
        throw new IllegalArgumentException("No LendingRule found for code: " + ruleCode);
    }

    /**
     * Find enum by human-readable rule name.
     * @param ruleName the human-readable rule name (e.g., "Repeated Lenders - High Risk")
     * @return the matching LendingRule enum constant
     * @throws IllegalArgumentException if no match found
     */
    public static LendingRule fromRuleName(String ruleName) {
        for (LendingRule rule : LendingRule.values()) {
            if (rule.ruleName.equalsIgnoreCase(ruleName)) {
                return rule;
            }
        }
        throw new IllegalArgumentException("No LendingRule found for name: " + ruleName);
    }

    /**
     * The enum constant name serves as the rule code (e.g., REPEATED_LENDERS_LOW_RISK).
     */
    public String getRuleCode() {
        return this.name();
    }

    @Override
    public String toString() {
        return getRuleCode() + " - " + ruleName;
    }
}
