package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.domain.AiRiskResult;
import com.abikananda.lendenclub.domain.AiRiskStatus;
import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.dto.AiRiskTestRequest;
import com.abikananda.lendenclub.service.AiRiskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRiskTestControllerTest {

    @Test
    void testEndpointMapsRequestAndReturnsProviderResult() {
        AiRiskService service = mock(AiRiskService.class);
        AiRiskResult expected = AiRiskResult.builder().status(AiRiskStatus.COMPLETED).build();
        when(service.evaluate(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

        AiRiskTestController controller = new AiRiskTestController(service);
        AiRiskTestRequest request = AiRiskTestRequest.builder()
                .creditScore(701)
                .lendenScore(800)
                .income(new BigDecimal("50000"))
                .loanAmount(new BigDecimal("5000"))
                .interestRate(new BigDecimal("36.48"))
                .tenure(4)
                .emi(new BigDecimal("1250"))
                .age(35)
                .borrowerType("SALARIED")
                .repeated(false)
                .trusted(false)
                .build();

        assertEquals(expected, controller.test(request).getBody());

        ArgumentCaptor<BorrowerFact> fact = ArgumentCaptor.forClass(BorrowerFact.class);
        verify(service).evaluate(fact.capture());
        assertEquals(701, fact.getValue().getCreditScore());
        assertEquals(new BigDecimal("5000"), fact.getValue().getLoanAmount());
        assertEquals("SALARIED", fact.getValue().getBorrowerType());
    }
}
