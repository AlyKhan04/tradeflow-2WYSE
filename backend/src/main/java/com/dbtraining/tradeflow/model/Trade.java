package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ============================================================================
 * Trade — TICKET-I017 + TICKET-I018 + TICKET-I025 + TICKET-I056
 * ============================================================================
 * WHAT:    Domain object representing a single trade. Central to the system.
 * HOW:     Plain POJO with private final fields and a fluent Builder.
 *          On Day 5 we convert it to a JPA @Entity.
 * WHY:     Immutability + Builder = thread-safe construction + a readable
 *          API at call sites. JPA needs a no-arg constructor — keep it
 *          protected so the Builder is still the only public way in.
 * OBSERVE: Trade t = Trade.builder().tradeRef("TRD-1").quantity(...).build();
 *          Two trades with the same tradeRef should be .equals().
 * ============================================================================
 *  TICKET-I017: define the fields and getters.
 *  TICKET-I018: add the Builder.
 *  TICKET-I025: override equals()/hashCode() using ONLY tradeRef.
 *  TICKET-I056: add JPA annotations — @Entity / @Table / @Id / @ManyToOne.
 * ============================================================================
 */
public class Trade extends BaseTrade {

    private final Long id;

    protected Trade() {
        super("", 0L, 0L, BigDecimal.ONE, BigDecimal.ONE, LocalDate.now(), TradeStatus.PENDING, Instant.now());
        this.id = null;
    }

    private Trade(Builder builder) {
        super(
                builder.tradeRef,
                builder.instrumentId,
                builder.counterpartyId,
                builder.quantity,
                builder.price,
                builder.tradeDate,
                builder.status,
                builder.createdAt);
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
        return "Generic Trade";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trade trade)) return false;
        return Objects.equals(tradeRef, trade.tradeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeRef);
    }

    @Override
    public String toString() {
        return String.format("Trade[%s | %d | %d | %s @ %s | %s | %s]",
                tradeRef,
                instrumentId,
                counterpartyId,
                quantity,
                price,
                tradeDate,
                getStatus());
    }

    public static final class Builder {
        private Long id;
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status = TradeStatus.PENDING;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder tradeRef(String tradeRef) {
            this.tradeRef = tradeRef;
            return this;
        }

        public Builder instrumentId(Long instrumentId) {
            this.instrumentId = instrumentId;
            return this;
        }

        public Builder counterpartyId(Long counterpartyId) {
            this.counterpartyId = counterpartyId;
            return this;
        }

        public Builder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder tradeDate(LocalDate tradeDate) {
            this.tradeDate = tradeDate;
            return this;
        }

        public Builder status(TradeStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

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
            if (price.signum() <= 0) {
                throw new IllegalStateException("price must be > 0");
            }
            return new Trade(this);
        }
    }
}
