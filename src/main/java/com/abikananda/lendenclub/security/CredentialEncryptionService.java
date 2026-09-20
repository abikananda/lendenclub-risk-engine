package com.abikananda.lendenclub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CredentialEncryptionService {

    public static final String PREFIX = "enc:v1:";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(@Value("${otp.credentials.encryption-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "OTP_CREDENTIAL_ENCRYPTION_KEY must contain a Base64-encoded 32-byte key");
        }
        final byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("OTP_CREDENTIAL_ENCRYPTION_KEY must be valid Base64", exception);
        }
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "OTP_CREDENTIAL_ENCRYPTION_KEY must decode to exactly 32 bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank() || isEncrypted(plaintext)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt OTP credential", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return encryptedValue;
        }
        if (!isEncrypted(encryptedValue)) {
            throw new IllegalStateException("OTP credential is not encrypted");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue.substring(PREFIX.length()));
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Encrypted OTP credential is malformed");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt OTP credential", exception);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
