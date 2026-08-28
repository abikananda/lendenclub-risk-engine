package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.LenderDto;
import com.abikananda.lendenclub.dto.LenderResponse;
import com.abikananda.lendenclub.dto.SessionDto;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.exception.ResourceNotFoundException;
import com.abikananda.lendenclub.repository.LenderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LenderService {

    private final LenderRepository lenderRepository;
    private final LendingSessionService sessionService;

    public LenderService(LenderRepository lenderRepository, LendingSessionService sessionService) {
        this.lenderRepository = lenderRepository;
        this.sessionService = sessionService;
    }

    @Transactional
    public LenderResponse getLenderAndStartSession() {
        Lender lender = lenderRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active lender found"));

        LendingSession session = sessionService.createSession(lender);

        return LenderResponse.builder()
                .sessionId(session.getSessionId())
                .lender(LenderDto.builder()
                        .lenderId(lender.getExternalLenderId())
                        .name(lender.getDisplayName())
                        .walletAmount(lender.getWalletAmount())
                        .username(lender.getUsername())
                        .mobileNumber(lender.getMobileNumber())
                        .otpUsername(lender.getOtpUsername())
                        .lendingRules(lender.getLendingRules())
                        .active(lender.getActive())
                        .build())
                .session(SessionDto.builder()
                        .status(session.getStatus())
                        .startedAt(session.getStartedAt())
                        .build())
                .build();
    }
}
