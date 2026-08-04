package com.dbtraining.tradeflow.exception;

/**
 * ============================================================================
 * TradeValidationException — TICKET-I032
 * ============================================================================
 * WHAT:    Checked exception thrown when a Trade fails validation.
 * HOW:     `extends Exception` (checked — callers must declare or catch).
 * WHY:     Validation errors are RECOVERABLE — callers will likely want to
 *          surface them to the user. Checked exceptions force that handling.
 *          (Compare: InsufficientDataException, which is UNRECOVERABLE and
 *           therefore unchecked.)
 * OBSERVE: TradeController catches this on Day 6 and returns 400 Bad Request.
 * ============================================================================
 *  TODO(TICKET-I032):
 *    - extend Exception (NOT RuntimeException)
 *    - inner enum Code { MISSING_FIELD, INVALID_VALUE, REFERENCE_NOT_FOUND }
 *    - constructor (Code, String message)
 *    - getCode() accessor
 * ============================================================================
 */
public class TradeValidationException extends Exception {

    public enum Code {
        MISSING_FIELD,
        INVALID_VALUE,
        REFERENCE_NOT_FOUND
    }

    private final Code code;

    public TradeValidationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    /** Legacy single-arg constructor — defaults to INVALID_VALUE. Prefer (Code, message). */
    public TradeValidationException(String message) {
        super(message);
        this.code = Code.INVALID_VALUE;
    }

    public TradeValidationException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}