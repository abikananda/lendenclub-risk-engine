package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.LendingRule;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.repository.LendingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LendingSessionConfigSnapshotTest {

    @Mock private LendingSessionRepository sessionRepository;

    @Test
    void storesConfiguredAmountAndRulesAtSessionCreation() {
        when(sessionRepository.save(any(LendingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lender lender = Lender.builder()
                .externalLenderId("LENDER-A")
                .build();
        List<LendingRule> rules = List.of(LendingRule.BULK_LENDERS);

        LendingSessionService service = new LendingSessionService(sessionRepository);
        service.createSession(lender, new BigDecimal("12500.00"), rules);

        ArgumentCaptor<LendingSession> captor = ArgumentCaptor.forClass(LendingSession.class);
        verify(sessionRepository).save(captor.capture());
        assertEquals(new BigDecimal("12500.00"), captor.getValue().getConfiguredInvestmentAmount());
        assertEquals(rules, captor.getValue().getConfiguredLendingRules());
    }
}
