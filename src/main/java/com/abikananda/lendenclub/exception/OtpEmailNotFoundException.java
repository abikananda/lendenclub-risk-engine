package com.abikananda.lendenclub.exception;

/**
 * Exception thrown when an OTP email is not found in Gmail inbox.
 * This is a retriable exception used by the resilience4j retry mechanism
 * to trigger automatic retries with exponential backoff.
 */
public class OtpEmailNotFoundException extends Exception {

    /**
     * Constructs OtpEmailNotFoundException with a detail message.
     *
     * @param message the detail message
     */
    public OtpEmailNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs OtpEmailNotFoundException with a detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public OtpEmailNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
