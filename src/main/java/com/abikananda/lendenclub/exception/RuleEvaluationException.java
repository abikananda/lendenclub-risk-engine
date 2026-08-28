package com.abikananda.lendenclub.exception;

public class RuleEvaluationException extends RuntimeException {
    private final String loanId;
    private final String ruleName;

    public RuleEvaluationException(String loanId, String ruleName, Throwable cause) {
        super("Failed to evaluate rule '" + ruleName + "' for loan " + loanId, cause);
        this.loanId = loanId;
        this.ruleName = ruleName;
    }

    public String getLoanId() {
        return loanId;
    }

    public String getRuleName() {
        return ruleName;
    }
}
