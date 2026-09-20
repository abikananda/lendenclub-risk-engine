package com.abikananda.lendenclub.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityValidator implements InitializingBean {

    private final Environment environment;
    private final boolean authenticationEnabled;
    private final String apiKey;

    public ProductionSecurityValidator(Environment environment,
                                       @Value("${backend.auth.enabled:true}") boolean authenticationEnabled,
                                       @Value("${backend.auth.api-key:}") String apiKey) {
        this.environment = environment;
        this.authenticationEnabled = authenticationEnabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public void afterPropertiesSet() {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        if (!authenticationEnabled) {
            throw new IllegalStateException("Backend API authentication cannot be disabled in the prod profile");
        }
        if (apiKey.isBlank()) {
            throw new IllegalStateException("BACKEND_API_KEY must be configured in the prod profile");
        }
    }
}
