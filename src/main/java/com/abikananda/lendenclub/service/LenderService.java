package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.LenderDto;
import com.abikananda.lendenclub.dto.LenderResponse;
import com.abikananda.lendenclub.dto.SessionDto;
import com.abikananda.lendenclub.entity.InvestmentConfig;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.exception.ResourceNotFoundException;
import com.abikananda.lendenclub.repository.InvestmentConfigRepository;
import com.abikananda.lendenclub.repository.LenderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LenderService {

    private final LenderRepository lenderRepository;
    private final InvestmentConfigRepository investmentConfigRepository;
    private final LendingSessionService sessionService;

    public LenderService(LenderRepository lenderRepository,
                         InvestmentConfigRepository investmentConfigRepository,
                         LendingSessionService sessionService) {
        this.lenderRepository = lenderRepository;
        this.investmentConfigRepository = investmentConfigRepository;
        this.sessionService = sessionService;
    }

    @Transactional
    public LenderResponse getLenderAndStartSession() {
        return getLenderAndStartSession(null);
    }

    @Transactional
    public LenderResponse getLenderAndStartSession(String externalLenderId) {
        Lender lender = externalLenderId == null || externalLenderId.isBlank()
                ? lenderRepository.findFirstByActiveTrue()
                    .orElseThrow(() -> new ResourceNotFoundException("No active lender found"))
                : lenderRepository.findByExternalLenderId(externalLenderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lender not found: " + externalLenderId));

        if (!Boolean.TRUE.equals(lender.getActive())) {
            throw new IllegalStateException("Lender is disabled: " + lender.getExternalLenderId());
        }

        InvestmentConfig config = investmentConfigRepository.findByLenderIdAndEnabledTrue(lender.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No enabled investment config found for lender: " + lender.getExternalLenderId()));

        if (config.getInvestmentAmount() == null || config.getInvestmentAmount().signum() <= 0) {
            throw new IllegalStateException("Investment amount must be greater than zero for lender: " + lender.getExternalLenderId());
        }
        if (config.getLendingRules() == null || config.getLendingRules().isEmpty()) {
            throw new IllegalStateException("At least one lending rule is required for lender: " + lender.getExternalLenderId());
        }

        LendingSession session = sessionService.createSession(
                lender,
                config.getInvestmentAmount(),
                config.getLendingRules());

        return LenderResponse.builder()
                .sessionId(session.getSessionId())
                .lender(LenderDto.builder()
                        .lenderId(lender.getExternalLenderId())
                        .name(lender.getDisplayName())
                        .walletAmount(config.getInvestmentAmount())
                        .username(lender.getUsername())
                        .mobileNumber(lender.getMobileNumber())
                        .otpUsername(lender.getOtpUsername())
                        .lendingRules(config.getLendingRules())
                        .active(lender.getActive())
                        .build())
                .session(SessionDto.builder()
                        .status(session.getStatus())
                        .startedAt(session.getStartedAt())
                        .build())
                .build();
    }
}
