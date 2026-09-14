package com.abikananda.lendenclub.domain;

public enum AiMode {
    OFF, SHADOW, GUARDRAIL;

    public static AiMode from(String value) {
        if (value == null || value.isBlank()) return OFF;
        return valueOf(value.trim().toUpperCase());
    }
}
