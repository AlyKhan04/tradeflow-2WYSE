package com.dbtraining.tradeflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@MappedSuperclass
public abstract class BaseTrade {

    @Column(name = "trade_ref", nullable = false, unique = true)
    protected String tradeRef;

    @Column(name = "instrument_id", nullable = false)
    protected Long instrumentId;

    @Column(name = "counterparty_id", nullable = false)
    protected Long counterpartyId;

    @Column(name = "quantity", nullable = false)
    protected BigDecimal quantity;

    @Column(name = "price", nullable = false)
    protected BigDecimal price;

    @Column(name = "trade_date", nullable = false)
    protected LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    protected TradeStatus status;

    @Column(name = "created_at")
    protected Instant createdAt;

    protected BaseTrade() {}

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