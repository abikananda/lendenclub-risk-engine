package com.abikananda.lendenclub.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class BorrowerProfileIdGenerator {

    static final String PREFIX = "BRW-";
    static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    static final int RANDOM_LENGTH = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder id = new StringBuilder(PREFIX.length() + RANDOM_LENGTH);
        id.append(PREFIX);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            id.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
