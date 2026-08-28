package com.abikananda.lendenclub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyAuthenticationFilterTest {

    private static final String API_KEY = "test-secret-key";

    @Test
    void disabledAuthenticationAllowsApiRequestWithoutKey() throws Exception {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(false, "", "X-API-Key", new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lender/data");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void enabledAuthenticationRejectsMissingKey() throws Exception {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(true, API_KEY, "X-API-Key", new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lender/data");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    void enabledAuthenticationAcceptsMatchingKey() throws Exception {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(true, API_KEY, "X-API-Key", new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/borrower/evaluate/BULK_LENDERS");
        request.addHeader("X-API-Key", API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void nonApiEndpointRemainsAvailableWithoutKey() throws Exception {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(true, API_KEY, "X-API-Key", new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(invoked.get());
    }

    @Test
    void enabledAuthenticationRequiresConfiguredKey() {
        assertThrows(
                IllegalStateException.class,
                () -> new ApiKeyAuthenticationFilter(true, " ", "X-API-Key", new ObjectMapper()));
    }
}
