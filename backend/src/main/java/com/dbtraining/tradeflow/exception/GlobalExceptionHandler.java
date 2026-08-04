package com.dbtraining.tradeflow.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================================
 * GlobalExceptionHandler — TICKET-I075
 * ============================================================================
 * WHAT:    @RestControllerAdvice that converts thrown exceptions into a
 *          consistent JSON error envelope.
 * HOW:     One @ExceptionHandler method per exception type, returning a
 *          ResponseEntity<Map<...>>.
 * WHY:     Every API consumer (React, Postman, tests) gets the same shape:
 *          { "code", "message", "timestamp", "path" }. No stack traces in
 *          production. Easier to consume on the frontend.
 * OBSERVE: POST a trade with missing fields → response body has these 4
 *          keys, HTTP 400.
 * ============================================================================
 *  TODO(TICKET-I075):
 *    - handle TradeValidationException → 400
 *    - handle TradeNotFoundException → 404
 *    - handle MethodArgumentNotValidException → 400 with field details
 *    - handle generic Exception → 500 (last-resort catch-all)
 *
 *  HINT: define a small `record ErrorEnvelope(...)` and return it from
 *        each handler — cleaner than Map<String,Object>.
 * ============================================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorEnvelope(String code, String message,
                                Map<String,String> details,
                                String timestamp, String path) {}

    @ExceptionHandler(TradeNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> notFound(TradeNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "TRADE_NOT_FOUND", ex.getMessage(), null, req);
    }

    @ExceptionHandler(TradeValidationException.class)
    public ResponseEntity<ErrorEnvelope> tradeValidation(TradeValidationException ex,
                                                         HttpServletRequest req) {
        String code = "VALIDATION_" + ex.getCode().name();
        return build(HttpStatus.BAD_REQUEST, code, ex.getMessage(), null, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> beanValidation(MethodArgumentNotValidException ex,
                                                        HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (var fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(),
                    fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more fields are invalid", fieldErrors, req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorEnvelope> dataIntegrity(DataIntegrityViolationException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "Unique constraint or referential integrity violation", null, req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorEnvelope> illegalState(IllegalStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorEnvelope> badArg(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null, req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient role for this resource", null, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> uncaught(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected error — see server logs", null, req);
    }

    private static ResponseEntity<ErrorEnvelope> build(HttpStatus status, String code,
                                                       String message,
                                                       Map<String,String> details,
                                                       HttpServletRequest req) {
        ErrorEnvelope body = new ErrorEnvelope(code, message, details,
                Instant.now().toString(),
                req != null ? req.getRequestURI() : null);
        return ResponseEntity.status(status).body(body);
    }
}