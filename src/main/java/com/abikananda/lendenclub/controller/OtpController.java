package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.dto.OtpResponse;
import com.abikananda.lendenclub.service.OtpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @GetMapping("/fetchOtp/{sessionId}")
    public ResponseEntity<OtpResponse> fetchOtp(@PathVariable String sessionId) {
        OtpResponse response = otpService.fetchOtpWithRetry(sessionId);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
