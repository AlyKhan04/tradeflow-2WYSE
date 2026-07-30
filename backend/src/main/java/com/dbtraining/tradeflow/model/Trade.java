package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Trade — generic trade type that shares the common BaseTrade fields.
 * Immutable from the outside: construction goes through the Builder.
 */
@Entity
@Table(name = "trades")
public class Trade extends BaseTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Trade() {}

    private Trade(Builder builder) {
        super(builder.tradeRef, builder.instrumentId, builder.counterpartyId,
                builder.quantity, builder.price, builder.tradeDate,
                builder.status, builder.createdAt);
        this.id = builder.id;
    }

    public Long getId() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String assetClassDescription() {
        return "Generic trade";
    }

    public static final class Builder {
        private Long id;
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;

        public Builder id(Long v)                { this.id = v;             return this; }
        public Builder tradeRef(String v)        { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)      { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)    { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)    { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)       { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)    { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)     { this.status = v;         return this; }
        public Builder createdAt(Instant v)      { this.createdAt = v;      return this; }

        public Trade build() {
            Objects.requireNonNull(tradeRef,       "tradeRef required");
            Objects.requireNonNull(instrumentId,   "instrumentId required");
            Objects.requireNonNull(counterpartyId, "counterpartyId required");
            Objects.requireNonNull(quantity,       "quantity required");
            Objects.requireNonNull(price,          "price required");
            Objects.requireNonNull(tradeDate,      "tradeDate required");
            if (quantity.signum() <= 0) throw new IllegalStateException("quantity must be > 0");
            if (price.signum() < 0)    throw new IllegalStateException("price must be >= 0");
            return new Trade(this);
        }
    }
}