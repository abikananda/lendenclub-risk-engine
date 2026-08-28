package com.abikananda.lendenclub.exception;

import com.abikananda.lendenclub.dto.ApiErrorResponse;
import com.abikananda.lendenclub.util.CorrelationIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidSessionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSession(InvalidSessionException ex, HttpServletRequest request) {
        log.error("Invalid Session Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "INVALID_SESSION", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.error("Resource Not Found Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RuleEvaluationException.class)
    public ResponseEntity<ApiErrorResponse> handleRuleEvaluation(RuleEvaluationException ex, HttpServletRequest request) {
        log.error("Rule evaluation failed loanId={} rule={}", ex.getLoanId(), ex.getRuleName(), ex);
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "RULE_EVALUATION_FAILED",
                "Unable to evaluate borrower safely for rule " + ex.getRuleName(),
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String msg = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getField() + " " + ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed";
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unhandled Exception at path {}: ", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String errorCode, String message, String path) {
        return ResponseEntity.status(status).body(
                ApiErrorResponse.builder()
                        .timestamp(OffsetDateTime.now())
                        .status(status.value())
                        .error(errorCode)
                        .message(message)
                        .path(path)
                        .correlationId(CorrelationIdUtil.getCorrelationId())
                        .build()
        );
    }
}
