package com.abikananda.lendenclub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    public static final String API_KEY_SCHEME = "backendApiKey";

    @Bean
    public OpenAPI lendingRiskEngineOpenApi(
            @Value("${backend.auth.header:X-API-Key}") String headerName) {
        SecurityScheme apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(headerName)
                .description("Backend API key configured through BACKEND_API_KEY");

        return new OpenAPI()
                .info(new Info()
                        .title("LenDenClub Risk Engine API")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME, apiKeyScheme))
                .addSecurityItem(new SecurityRequirement()
                        .addList(API_KEY_SCHEME));
    }
}
