package com.abikananda.lendenclub.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorrowerProfileIdGeneratorTest {

    @Test
    void generatesReadableProfileIdsFromConfiguredAlphabet() {
        BorrowerProfileIdGenerator generator = new BorrowerProfileIdGenerator();

        for (int i = 0; i < 1_000; i++) {
            String id = generator.generate();
            assertTrue(id.matches("BRW-[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{10}"));
        }
    }

    @Test
    void generatedSampleHasNoDuplicates() {
        BorrowerProfileIdGenerator generator = new BorrowerProfileIdGenerator();
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < 10_000; i++) {
            ids.add(generator.generate());
        }

        assertEquals(10_000, ids.size());
    }
}
