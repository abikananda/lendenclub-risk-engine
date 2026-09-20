package com.abikananda.lendenclub.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSecurityValidatorTest {

    @Test
    void productionRejectsDisabledAuthentication() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        ProductionSecurityValidator validator =
                new ProductionSecurityValidator(environment, false, "configured-key");

        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void productionRejectsMissingApiKey() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        ProductionSecurityValidator validator =
                new ProductionSecurityValidator(environment, true, " ");

        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void productionAcceptsEnabledAuthenticationWithKey() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        ProductionSecurityValidator validator =
                new ProductionSecurityValidator(environment, true, "configured-key");

        assertDoesNotThrow(validator::afterPropertiesSet);
    }
}
