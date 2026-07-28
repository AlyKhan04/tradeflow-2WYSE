package com.dbtraining.tradeflow.exception;

/**
 * ============================================================================
 * JdbcException — lightweight runtime wrapper for checked SQL exceptions.
 * ============================================================================
 * WHAT:    Wraps SQLException so DAO callers do not need to declare checked
 *          exceptions throughout the service layer.
 * WHY:     Keeps JDBC plumbing simple while still preserving the root cause.
 * ============================================================================
 */
public class JdbcException extends RuntimeException {

    public JdbcException(String message) {
        super(message);
    }

    public JdbcException(String message, Throwable cause) {
        super(message, cause);
    }
}
