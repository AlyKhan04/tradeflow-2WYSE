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

    // TODO(TICKET-I028): protected final fields for the shared values
    //   tradeRef, instrumentId, counterpartyId, quantity, price, tradeDate,
    //   status, createdAt.

    // TODO(TICKET-I028): protected constructor (subclasses call super(...)).

    // TODO(TICKET-I028): public getters.

    /**
     * Each asset class returns its own description for logs/UI.
     * EquityTrade → "Equity on XETRA"
     * FXTrade     → "FX EUR/USD"
     * BondTrade   → "Bond coupon 4.50% mat 2030-06-15"
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

    protected BaseTrade(String tradeRef, Long instrumentId, Long counterpartyId,
                        BigDecimal quantity, BigDecimal price, LocalDate tradeDate,
                        TradeStatus status, Instant createdAt) {
        this.tradeRef       = Objects.requireNonNull(tradeRef,       "tradeRef required");
        this.instrumentId   = Objects.requireNonNull(instrumentId,   "instrumentId required");
        this.counterpartyId = Objects.requireNonNull(counterpartyId, "counterpartyId required");
        this.quantity       = Objects.requireNonNull(quantity,       "quantity required");
        this.price          = Objects.requireNonNull(price,          "price required");
        this.tradeDate      = Objects.requireNonNull(tradeDate,      "tradeDate required");
        this.status         = status != null ? status : TradeStatus.PENDING;
        this.createdAt      = createdAt != null ? createdAt : Instant.now();
        if (quantity.signum() <= 0) throw new IllegalStateException("quantity must be > 0");
        if (price.signum() < 0)     throw new IllegalStateException("price must be >= 0");
    }

    public String getTradeRef()       { return tradeRef; }
    public Long getInstrumentId()     { return instrumentId; }
    public Long getCounterpartyId()   { return counterpartyId; }
    public BigDecimal getQuantity()   { return quantity; }
    public BigDecimal getPrice()      { return price; }
    public LocalDate getTradeDate()   { return tradeDate; }
    public TradeStatus getStatus()    { return status; }
    public Instant getCreatedAt()     { return createdAt; }

    public BigDecimal getNotional() { return quantity.multiply(price); }

    public abstract String assetClassDescription();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseTrade other)) return false;
        return Objects.equals(tradeRef, other.tradeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeRef);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[" + tradeRef
                + " | " + assetClassDescription()
                + " | " + quantity + " @ " + price
                + " | " + tradeDate
                + " | " + status + "]";
    }
}