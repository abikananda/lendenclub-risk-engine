package com.abikananda.lendenclub.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialEncryptionServiceTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void encryptsWithRandomIvAndDecrypts() {
        CredentialEncryptionService service = new CredentialEncryptionService(KEY);

        String first = service.encrypt("app-password");
        String second = service.encrypt("app-password");

        assertTrue(first.startsWith(CredentialEncryptionService.PREFIX));
        assertNotEquals(first, second);
        assertEquals("app-password", service.decrypt(first));
        assertEquals("app-password", service.decrypt(second));
    }

    @Test
    void rejectsMissingOrInvalidKeys() {
        assertThrows(IllegalStateException.class, () -> new CredentialEncryptionService(""));
        assertThrows(IllegalStateException.class, () -> new CredentialEncryptionService("not-base64"));
        assertThrows(IllegalStateException.class,
                () -> new CredentialEncryptionService("c2hvcnQ="));
    }

    @Test
    void rejectsPlaintextDuringDecryption() {
        CredentialEncryptionService service = new CredentialEncryptionService(KEY);
        assertThrows(IllegalStateException.class, () -> service.decrypt("plain-password"));
    }
}
