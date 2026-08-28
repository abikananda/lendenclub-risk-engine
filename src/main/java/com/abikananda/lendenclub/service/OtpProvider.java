package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.config.GmailConfig;

import java.util.Optional;

public interface OtpProvider {
    Optional<String> fetchLatestOtp(GmailConfig gmailConfig);
}
