package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.LendingRule;
import com.abikananda.lendenclub.domain.LendingSessionStatus;
import com.abikananda.lendenclub.dto.LenderResponse;
import com.abikananda.lendenclub.entity.InvestmentConfig;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.repository.InvestmentConfigRepository;
import com.abikananda.lendenclub.repository.LenderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LenderServiceInvestmentConfigTest {

    @Mock private LenderRepository lenderRepository;
    @Mock private InvestmentConfigRepository investmentConfigRepository;
    @Mock private LendingSessionService sessionService;

    @Test
    void startsRequestedLenderUsingInvestmentConfigSnapshot() {
        Lender lender = Lender.builder()
                .id(10L)
                .externalLenderId("LENDER-A")
                .displayName("Lender A")
                .walletAmount(new BigDecimal("99999.00"))
                .username("lender-a")
                .mobileNumber("9000000001")
                .otpUsername("otp-a")
                .active(true)
                .lendingRules(List.of(LendingRule.GOOD_LENDERS))
                .build();

        List<LendingRule> configuredRules = List.of(
                LendingRule.REPEATED_LENDERS_HIGH_RISK,
                LendingRule.BULK_LENDERS);
        InvestmentConfig config = InvestmentConfig.builder()
                .lender(lender)
                .investmentAmount(new BigDecimal("10000.00"))
                .lendingRules(configuredRules)
                .enabled(true)
                .build();

        LendingSession session = LendingSession.builder()
                .sessionId("LS-TEST-A")
                .lender(lender)
                .configuredInvestmentAmount(config.getInvestmentAmount())
                .configuredLendingRules(configuredRules)
                .status(LendingSessionStatus.STARTED)
                .startedAt(OffsetDateTime.now())
                .lastActivityAt(OffsetDateTime.now())
                .build();

        when(lenderRepository.findByExternalLenderId("LENDER-A")).thenReturn(Optional.of(lender));
        when(investmentConfigRepository.findByLender_IdAndEnabledTrue(10L)).thenReturn(Optional.of(config));
        when(sessionService.createSession(lender, config.getInvestmentAmount(), configuredRules)).thenReturn(session);

        LenderService service = new LenderService(lenderRepository, investmentConfigRepository, sessionService);
        LenderResponse response = service.getLenderAndStartSession("LENDER-A");

        assertEquals("LS-TEST-A", response.getSessionId());
        assertEquals("LENDER-A", response.getLender().getLenderId());
        assertEquals(new BigDecimal("10000.00"), response.getLender().getWalletAmount());
        assertEquals(configuredRules, response.getLender().getLendingRules());
        verify(sessionService).createSession(lender, new BigDecimal("10000.00"), configuredRules);
    }
}
