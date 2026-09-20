package com.abikananda.lendenclub.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyOtpCredentialMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyOtpCredentialMigration.class);
    private final JdbcTemplate jdbcTemplate;
    private final CredentialEncryptionService encryptionService;

    public LegacyOtpCredentialMigration(JdbcTemplate jdbcTemplate,
                                        CredentialEncryptionService encryptionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<LegacyCredential> credentials = jdbcTemplate.query(
                "SELECT id, otp_password FROM lender WHERE otp_password NOT LIKE 'enc:v1:%'",
                (resultSet, rowNumber) -> new LegacyCredential(
                        resultSet.getLong("id"), resultSet.getString("otp_password")));
        for (LegacyCredential credential : credentials) {
            jdbcTemplate.update(
                    "UPDATE lender SET otp_password = ? WHERE id = ? AND otp_password = ?",
                    encryptionService.encrypt(credential.password()), credential.id(), credential.password());
        }
        if (!credentials.isEmpty()) {
            log.info("Encrypted {} legacy lender OTP credential(s)", credentials.size());
        }
    }

    private record LegacyCredential(long id, String password) {
    }
}
