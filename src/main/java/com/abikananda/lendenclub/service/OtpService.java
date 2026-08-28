package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.config.GmailConfig;
import com.abikananda.lendenclub.domain.LendingSessionStatus;
import com.abikananda.lendenclub.dto.OtpResponse;
import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.entity.LendingSession;
import com.abikananda.lendenclub.repository.LenderRepository;
import com.abikananda.lendenclub.repository.LendingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpProvider otpProvider;
    private final int maxRetries;
    private final long retryDelayMs;
    LendingSessionRepository sessionRepository;
    LenderRepository lenderRepository;

    public OtpService(OtpProvider otpProvider,
                      @Value("${otp.max-retries:5}") int maxRetries,
                      @Value("${otp.retry-delay-ms:2000}") long retryDelayMs,LendingSessionRepository sessionRepository, LenderRepository lenderRepository) {
        this.sessionRepository = sessionRepository;
        this.lenderRepository = lenderRepository;
        this.otpProvider = otpProvider;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    public OtpResponse fetchOtpWithRetry(String sessionId) {
        log.info("OTP fetch requested for session={}", sessionId);
        GmailConfig gmailConfig = getSessionAndFetchLenderConfig(sessionId);
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            log.info("Searching Gmail for OTP attempt={}", attempt);
            var otpOpt = otpProvider.fetchLatestOtp(gmailConfig);

            if (otpOpt.isPresent()) {
                log.info("OTP retrieved successfully");
                return OtpResponse.builder()
                        .success(true)
                        .otp(otpOpt.get())
                        .message("OTP fetched successfully")
                        .build();
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.warn("OTP not received after max attempts={}", maxRetries);
        return OtpResponse.builder()
                .success(false)
                .otp(null)
                .message("OTP not received after " + maxRetries + " attempts")
                .build();
    }

    private GmailConfig getSessionAndFetchLenderConfig(String sessionId) {
        Optional<LendingSession> sessionOpt = sessionRepository.findBySessionIdAndStatus(sessionId, LendingSessionStatus.STARTED);
        if (sessionOpt.isEmpty()) {
            log.error("No active session found for sessionId={}", sessionId);
            throw new RuntimeException("No active session found for sessionId=" + sessionId);
        }
        LendingSession session = sessionOpt.get();

        GmailConfig gmailConfig = new GmailConfig();
        gmailConfig.setUsername(session.getLender().getOtpUsername());
        gmailConfig.setPassword(session.getLender().getOtpPassword());
        return gmailConfig;
    }
}
