package com.abikananda.lendenclub.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

@Component
@Converter(autoApply = false)
public class OtpPasswordConverter implements AttributeConverter<String, String> {

    private final CredentialEncryptionService encryptionService;

    public OtpPasswordConverter(CredentialEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return databaseValue;
        }
        return encryptionService.isEncrypted(databaseValue)
                ? encryptionService.decrypt(databaseValue)
                : databaseValue;
    }
}
