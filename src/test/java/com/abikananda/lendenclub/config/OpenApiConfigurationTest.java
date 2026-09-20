package com.abikananda.lendenclub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigurationTest {

    @Test
    void configuresApiKeyAuthorizeHeaderForSwagger() {
        OpenAPI openApi = new OpenApiConfiguration().lendingRiskEngineOpenApi("X-API-Key");

        SecurityScheme scheme = openApi.getComponents()
                .getSecuritySchemes()
                .get(OpenApiConfiguration.API_KEY_SCHEME);

        assertNotNull(scheme);
        assertEquals(SecurityScheme.Type.APIKEY, scheme.getType());
        assertEquals(SecurityScheme.In.HEADER, scheme.getIn());
        assertEquals("X-API-Key", scheme.getName());
        assertTrue(openApi.getSecurity().get(0)
                .containsKey(OpenApiConfiguration.API_KEY_SCHEME));
    }
}
