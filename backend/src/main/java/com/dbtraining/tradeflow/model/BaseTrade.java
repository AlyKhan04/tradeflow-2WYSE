package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ============================================================================
 * BaseTrade — TICKET-I028
 * ============================================================================
 * WHAT:    Abstract superclass for asset-class-specific trade types
 *          (EquityTrade, FXTrade, BondTrade).
 * HOW:     Holds the common fields. Concrete classes add asset-class-specific
 *          fields and override the abstract describer.
 * WHY:     Demonstrates inheritance + polymorphism in real domain terms,
 *          and gives `ReconciliationService` one type to operate on.
 * OBSERVE: You CANNOT do `new BaseTrade(...)` — only the subclasses.
 *
 * GOTCHA:  Read "Effective Java" Item 18 — favour composition over inheritance.
 *          Discuss with your team: is BaseTrade the right call, or would a
 *          single Trade with an AssetClass enum + composition (e.g.
 *          AssetSpecificDetails) be cleaner? Document your choice in the PR.
 * ============================================================================
 */
public abstract class BaseTrade {

    protected final String tradeRef;
    protected final Long instrumentId;
    protected final Long counterpartyId;
    protected final BigDecimal quantity;
    protected final BigDecimal price;
    protected final LocalDate tradeDate;
    protected final TradeStatus status;
    protected final Instant createdAt;

    protected BaseTrade(String tradeRef,
                        Long instrumentId,
                        Long counterpartyId,
                        BigDecimal quantity,
                        BigDecimal price,
                        LocalDate tradeDate,
                        TradeStatus status,
                        Instant createdAt) {
        this.tradeRef = Objects.requireNonNull(tradeRef, "tradeRef is required");
        this.instrumentId = Objects.requireNonNull(instrumentId, "instrumentId is required");
        this.counterpartyId = Objects.requireNonNull(counterpartyId, "counterpartyId is required");
        this.quantity = Objects.requireNonNull(quantity, "quantity is required");
        this.price = Objects.requireNonNull(price, "price is required");
        this.tradeDate = Objects.requireNonNull(tradeDate, "tradeDate is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getTradeRef() {
        return tradeRef;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public Long getCounterpartyId() {
        return counterpartyId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getNotional() {
        return quantity.multiply(price);
    }

    public abstract String assetClassDescription();
}
