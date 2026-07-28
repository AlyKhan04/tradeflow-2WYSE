package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * DiscrepancyType — TICKET-I021
 * ============================================================================
 * WHAT:    The reason code for a reconciliation break.
 * WHY:     Ops users need to know WHY a trade broke — not just that it did.
 * OBSERVE: Day 3's ReconciliationService.matchTrades() decides which
 *          discrepancy type to flag based on field-by-field comparison.
 * ============================================================================
 */
public enum DiscrepancyType {

    PRICE_MISMATCH,
    QUANTITY_MISMATCH,
    DATE_MISMATCH,
    MISSING_TRADE;

    public String describe() {
        return switch (this) {
            case PRICE_MISMATCH -> "Price does not match counterparty record";
            case QUANTITY_MISMATCH -> "Quantity does not match counterparty record";
            case DATE_MISMATCH -> "Trade date does not match counterparty record";
            case MISSING_TRADE -> "Trade exists on one side but not the other";
        };
    }
}
