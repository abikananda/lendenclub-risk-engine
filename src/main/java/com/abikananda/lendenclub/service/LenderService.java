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
    private final LenderExecutionLeaseService leaseService;

    public LenderService(LenderRepository lenderRepository,
                         InvestmentConfigRepository investmentConfigRepository,
                         LendingSessionService sessionService,
                         LenderExecutionLeaseService leaseService) {
        this.lenderRepository = lenderRepository;
        this.investmentConfigRepository = investmentConfigRepository;
        this.sessionService = sessionService;
        this.leaseService = leaseService;
    }

    @Transactional(readOnly = true)
    public LenderDto getLenderConfig(String username) {
        ResolvedLender resolved = resolve(username);
        return toLenderDto(resolved.lender(), resolved.config());
    }

    @Transactional
    public LenderResponse startSession(String username, String ownerId) {
        ResolvedLender resolved = resolve(username);
        Lender lender = resolved.lender();
        InvestmentConfig config = resolved.config();

        LendingSession session = sessionService.createSession(
                lender,
                config.getInvestmentAmount(),
                config.getLendingRules());

        // Same transaction as session creation: if the lease cannot be acquired,
        // the newly-created session is rolled back and no orphan STARTED session remains.
        leaseService.acquire(lender, session.getSessionId(), ownerId);

        return LenderResponse.builder()
                .sessionId(session.getSessionId())
                .lender(toLenderDto(lender, config))
                .session(SessionDto.builder()
                        .status(session.getStatus())
                        .startedAt(session.getStartedAt())
                        .build())
                .build();
    }

    private ResolvedLender resolve(String username) {
        Lender lender = username == null || username.isBlank()
                ? lenderRepository.findFirstByActiveTrue()
                    .orElseThrow(() -> new ResourceNotFoundException("No active lender found"))
                : lenderRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Lender not found for username: " + username));

        if (!Boolean.TRUE.equals(lender.getActive())) {
            throw new IllegalStateException("Lender is disabled: " + lender.getUsername());
        }

        InvestmentConfig config = investmentConfigRepository.findByLender_IdAndEnabledTrue(lender.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No enabled investment config found for lender: " + lender.getUsername()));

        if (config.getInvestmentAmount() == null || config.getInvestmentAmount().signum() <= 0) {
            throw new IllegalStateException("Investment amount must be greater than zero for lender: " + lender.getUsername());
        }
        if (config.getLendingRules() == null || config.getLendingRules().isEmpty()) {
            throw new IllegalStateException("At least one lending rule is required for lender: " + lender.getUsername());
        }

        return new ResolvedLender(lender, config);
    }

    private LenderDto toLenderDto(Lender lender, InvestmentConfig config) {
        return LenderDto.builder()
                .lenderId(lender.getExternalLenderId())
                .name(lender.getDisplayName())
                .walletAmount(config.getInvestmentAmount())
                .username(lender.getUsername())
                .mobileNumber(lender.getMobileNumber())
                .otpUsername(lender.getOtpUsername())
                .lendingRules(config.getLendingRules())
                .active(lender.getActive())
                .build();
    }

    private record ResolvedLender(Lender lender, InvestmentConfig config) {}
}
