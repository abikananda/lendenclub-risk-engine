package com.abikananda.lendenclub.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.*;
import java.util.stream.Collectors;

@Converter(autoApply = false)
public class LendingRuleListConverter implements AttributeConverter<List<LendingRule>, String> {

    private static final String SEP = ",";

    @Override
    public String convertToDatabaseColumn(List<LendingRule> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        return attribute.stream()
                .map(LendingRule::name)
                .collect(Collectors.joining(SEP));
    }

    @Override
    public List<LendingRule> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        String[] parts = dbData.split(SEP);
        List<LendingRule> list = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) continue;
            try {
                list.add(LendingRule.valueOf(trimmed));
            } catch (IllegalArgumentException ex) {
                // unknown value: skip or handle as needed
            }
        }
        return list;
    }
}