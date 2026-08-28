package com.abikananda.lendenclub.config;

import com.abikananda.lendenclub.util.CorrelationIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final boolean enabled;
    private final String expectedApiKey;
    private final String headerName;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(
            @Value("${backend.auth.enabled:false}") boolean enabled,
            @Value("${backend.auth.api-key:}") String expectedApiKey,
            @Value("${backend.auth.header:X-API-Key}") String headerName,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.expectedApiKey = expectedApiKey == null ? "" : expectedApiKey.trim();
        this.headerName = headerName == null || headerName.isBlank() ? "X-API-Key" : headerName.trim();
        this.objectMapper = objectMapper;

        if (enabled && this.expectedApiKey.isBlank()) {
            throw new IllegalStateException("backend.auth.api-key must be configured when backend.auth.enabled=true");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String suppliedApiKey = request.getHeader(headerName);

        if (!matches(suppliedApiKey)) {
            String correlationId = CorrelationIdUtil.getCorrelationId();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("X-Correlation-Id", correlationId);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", OffsetDateTime.now().toString());
            body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            body.put("error", "UNAUTHORIZED");
            body.put("message", "Missing or invalid backend API key");
            body.put("path", request.getRequestURI());
            body.put("correlationId", correlationId);

            log.warn("Rejected unauthorized backend API request method={} path={} correlationId={}",
                    request.getMethod(), request.getRequestURI(), correlationId);
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matches(String suppliedApiKey) {
        if (suppliedApiKey == null || suppliedApiKey.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedApiKey.getBytes(StandardCharsets.UTF_8),
                suppliedApiKey.trim().getBytes(StandardCharsets.UTF_8));
    }
}
