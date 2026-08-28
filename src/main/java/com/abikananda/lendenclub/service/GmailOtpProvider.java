package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.config.GmailConfig;
import com.abikananda.lendenclub.exception.OtpEmailNotFoundException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.search.*;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gmail-based OTP provider implementation.
 * Connects to Gmail via IMAP to fetch OTP emails with resilience4j retry mechanism.
 * Uses exponential backoff strategy for robustness.
 */
@Service
public class GmailOtpProvider implements OtpProvider {

    private static final Logger log = LoggerFactory.getLogger(GmailOtpProvider.class);
    private static final String GMAIL_HOST = "imap.gmail.com";
    private static final String OTP_SENDER = "noreply@lendenclub.com";
    private static final String OTP_SUBJECT = "LendenClub | OTP for Login";
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{6})\\b");
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long INITIAL_INTERVAL_MS = 5000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * Fetches the latest OTP from Gmail with automatic retries.
     * Returns Optional.empty() if no OTP is found after retry attempts.
     *
     * @return Optional containing the 6-digit OTP, or empty if not found
     */
    @Override
    public Optional<String> fetchLatestOtp(GmailConfig gmailConfig) {
        log.info("Initiating OTP fetch from Gmail");

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_RETRY_ATTEMPTS)
                .intervalFunction(
                        IntervalFunction.ofExponentialBackoff(
                                INITIAL_INTERVAL_MS,
                                BACKOFF_MULTIPLIER
                        )
                )
                .retryOnException(ex -> ex.getCause() instanceof OtpEmailNotFoundException)
                .build();

        Retry retry = Retry.of("gmail-otp-fetch", config);

        // Configure event listeners for retry monitoring
        retry.getEventPublisher()
                .onRetry(event -> {
                    log.warn("Retry attempt #{} — Reason: {}",
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getCause().getMessage());
                })
                .onSuccess(event -> {
                    log.info("OTP fetch successful");
                })
                .onError(event -> {
                    log.error("OTP fetch failed after {} attempts",
                            event.getNumberOfRetryAttempts());
                });

        Supplier<String> decoratedSupplier = Retry.decorateSupplier(retry, () -> {
            try {
                return fetchOtpFromGmail(gmailConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            String otp = decoratedSupplier.get();
            log.info("OTP successfully extracted from Gmail");
            return Optional.of(otp);
        } catch (Exception ex) {
            log.error("Failed to fetch OTP after all retries: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Attempts a single OTP fetch from Gmail IMAP.
     * Searches for unread emails from LendenClub with OTP subject.
     *
     * @return The extracted 6-digit OTP
     * @throws OtpEmailNotFoundException if no unread OTP email is found
     * @throws Exception if mail operations fail
     */
    private String fetchOtpFromGmail(GmailConfig gmailConfig) throws Exception {
        log.debug("Attempting to fetch OTP from Gmail IMAP");

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");

        Session session = Session.getDefaultInstance(properties);
        Store store = null;
        Folder inbox = null;

        try {
            store = session.getStore("imaps");
            store.connect(GMAIL_HOST, gmailConfig.getUsername(), gmailConfig.getPassword());
            log.debug("Connected to Gmail IMAP store");

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            log.debug("Opened INBOX folder");

            // Build search criteria: unread emails from LendenClub with OTP subject
            FlagTerm unseenFlag = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            FromStringTerm fromAddress = new FromStringTerm(OTP_SENDER);
            SubjectTerm subjectTerm = new SubjectTerm(OTP_SUBJECT);

            SearchTerm searchTerm = new AndTerm(new SearchTerm[]{ unseenFlag, fromAddress, subjectTerm });
            Message[] messages = inbox.search(searchTerm);

            if (messages.length == 0) {
                log.warn("No unread OTP emails found from {}", OTP_SENDER);
                throw new OtpEmailNotFoundException(
                    String.format("No unread OTP email found from %s with subject containing '%s'",
                            OTP_SENDER, OTP_SUBJECT)
                );
            }

            log.info("Found {} unread OTP email(s)", messages.length);

            // Process the most recent email
            Message recentMessage = messages[messages.length - 1];
            String emailContent = extractEmailText(recentMessage);
            log.debug("Email content extracted successfully");

            String otp = extractOtp(emailContent);
            if (otp == null) {
                log.error("OTP pattern not found in email content");
                throw new Exception("OTP not found inside email content");
            }

            // Mark email as read
            recentMessage.setFlag(Flags.Flag.SEEN, true);
            log.debug("Marked email as read");

            return otp;

        } finally {
            // Ensure proper resource cleanup
            if (inbox != null && inbox.isOpen()) {
                try {
                    inbox.close(true);
                    log.debug("Closed INBOX folder");
                } catch (MessagingException e) {
                    log.warn("Error closing inbox folder", e);
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                    log.debug("Closed Gmail IMAP connection");
                } catch (MessagingException e) {
                    log.warn("Error closing store connection", e);
                }
            }
        }
    }

    /**
     * Extracts plain text from email message.
     * Handles plain text, HTML, and multipart message formats.
     *
     * @param message The email message
     * @return Extracted text content
     * @throws Exception if message content extraction fails
     */
    private String extractEmailText(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("text/html")) {
            String html = (String) message.getContent();
            return Jsoup.parse(html).text();
        } else if (message.getContent() instanceof Multipart) {
            Multipart multipart = (Multipart) message.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    return part.getContent().toString();
                } else if (part.isMimeType("text/html")) {
                    String html = (String) part.getContent();
                    return Jsoup.parse(html).text();
                }
            }
        }
        return "";
    }

    /**
     * Extracts a 6-digit OTP from text using regex pattern matching.
     *
     * @param text The text to search for OTP
     * @return The extracted OTP code, or null if not found
     */
    private String extractOtp(String text) {
        Matcher matcher = OTP_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("No 6-digit OTP pattern found in email text");
        return null;
    }
}
