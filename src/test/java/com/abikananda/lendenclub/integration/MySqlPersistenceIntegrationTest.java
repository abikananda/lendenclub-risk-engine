package com.abikananda.lendenclub.integration;

import com.abikananda.lendenclub.domain.InvestmentStatus;
import com.abikananda.lendenclub.dto.InvestmentStatusRequest;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.repository.InvestmentRepository;
import com.abikananda.lendenclub.repository.LenderRepository;
import com.abikananda.lendenclub.repository.LendingSessionRepository;
import com.abikananda.lendenclub.service.InvestmentService;
import com.abikananda.lendenclub.service.LendingSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class MySqlPersistenceIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("lendenclub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
    }

    @Autowired
    private LenderRepository lenderRepository;

    @Autowired
    private LendingSessionService sessionService;

    @Autowired
    private LendingSessionRepository sessionRepository;

    @Autowired
    private InvestmentService investmentService;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Test
    void flywayCreatesSchemaAndDuplicateInvestmentIsIdempotent() {
        Lender lender = lenderRepository.save(Lender.builder()
                .externalLenderId("LENDER-TEST")
                .displayName("Integration Test Lender")
                .walletAmount(new BigDecimal("10000.00"))
                .username("integration-user")
                .mobileNumber("9999999999")
                .otpUsername("integration-otp")
                .otpPassword("test-password")
                .active(true)
                .build());

        LendingSession session = sessionService.createSession(lender);
        assertNotNull(session.getSessionId());

        InvestmentStatusRequest request = InvestmentStatusRequest.builder()
                .sessionId(session.getSessionId())
                .loanId("LOAN-INTEGRATION-1")
                .investmentAmount(new BigDecimal("250.00"))
                .status(InvestmentStatus.SUCCESS)
                .externalInvestmentId("EXT-INTEGRATION-1")
                .message("success")
                .build();

        investmentService.recordStatus(request);
        investmentService.recordStatus(request);

        assertEquals(1, investmentRepository.countBySessionId(session.getSessionId()));

        LendingSession persisted = sessionRepository.findBySessionId(session.getSessionId()).orElseThrow();
        assertEquals(1, persisted.getTotalInvestments());
        assertEquals(1, persisted.getSuccessfulInvestments());
        assertEquals(0, new BigDecimal("250.00").compareTo(persisted.getTotalAmountInvested()));
        assertEquals(lender.getId(), investmentRepository.findByExternalInvestmentId("EXT-INTEGRATION-1")
                .orElseThrow()
                .getLender()
                .getId());
    }
}
