// backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Trade — POJO mirroring the trades table.
 * Immutable from the outside: construction goes through the Builder (I018).
 */
public class Trade {

    private String tradeRef;
    private Long instrumentId;
    private Long counterpartyId;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDate tradeDate;
    private TradeStatus status;
    private Instant createdAt;

    // Package-private no-arg constructor — Builder is the public path.
    Trade() {}

    public String getTradeRef()         { return tradeRef; }
    public Long getInstrumentId()       { return instrumentId; }
    public Long getCounterpartyId()     { return counterpartyId; }
    public BigDecimal getQuantity()     { return quantity; }
    public BigDecimal getPrice()        { return price; }
    public LocalDate getTradeDate()     { return tradeDate; }
    public TradeStatus getStatus()      { return status; }
    public Instant getCreatedAt()       { return createdAt; }

    /** Notional = quantity * price. Computed; not stored. */
    public BigDecimal getNotional() {
        return quantity == null || price == null ? null : quantity.multiply(price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trade other)) return false;
        return Objects.equals(tradeRef, other.tradeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeRef);
    }

    @Override
    public String toString() {
        return "Trade[" + tradeRef
                + " | " + instrumentId
                + " | " + quantity + " @ " + price
                + " | " + tradeDate
                + " | " + status + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    private Trade(Builder builder) {
        this.tradeRef = builder.tradeRef;
        this.instrumentId = builder.instrumentId;
        this.counterpartyId = builder.counterpartyId;
        this.quantity = builder.quantity;
        this.price = builder.price;
        this.tradeDate = builder.tradeDate;
        this.status = builder.status != null ? builder.status : TradeStatus.PENDING;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }

    public static final class Builder {
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;

        public Builder tradeRef(String v)        { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)      { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)    { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)    { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)       { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)     { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)     { this.status = v;         return this; }
        public Builder createdAt(Instant v)      { this.createdAt = v;      return this; }

        public Trade build() {
            Objects.requireNonNull(tradeRef, "tradeRef required");
            Objects.requireNonNull(instrumentId, "instrumentId required");
            Objects.requireNonNull(counterpartyId, "counterpartyId required");
            Objects.requireNonNull(quantity, "quantity required");
            Objects.requireNonNull(price, "price required");
            Objects.requireNonNull(tradeDate, "tradeDate required");
            if (quantity.signum() <= 0) {
                throw new IllegalStateException("quantity must be > 0");
            }
            if (price.signum() < 0) {
                throw new IllegalStateException("price must be >= 0");
            }
            return new Trade(this);
        }
    }
}