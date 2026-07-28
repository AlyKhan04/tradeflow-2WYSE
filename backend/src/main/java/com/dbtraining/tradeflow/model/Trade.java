package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Trade — TICKET-I017 + TICKET-I018 + TICKET-I025 + TICKET-I056
 */
public class Trade {

    private final String tradeRef;
    private final Long instrumentId;
    private final Long counterpartyId;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final LocalDate tradeDate;
    private final TradeStatus status;
    private final Instant createdAt;

    /**
     * Protected no-arg constructor for frameworks (JPA/Hibernate) that require it.
     * Keep it protected so Builder remains the public construction path.
     */
    protected Trade() {
        this.tradeRef = null;
        this.instrumentId = null;
        this.counterpartyId = null;
        this.quantity = null;
        this.price = null;
        this.tradeDate = null;
        this.status = null;
        this.createdAt = null;
    }

    private Trade(Builder b) {
        this.tradeRef = b.tradeRef;
        this.instrumentId = b.instrumentId;
        this.counterpartyId = b.counterpartyId;
        this.quantity = b.quantity;
        this.price = b.price;
        this.tradeDate = b.tradeDate;
        this.status = b.status;
        this.createdAt = b.createdAt != null ? b.createdAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public String getTradeRef() { return tradeRef; }
    public Long getInstrumentId() { return instrumentId; }
    public Long getCounterpartyId() { return counterpartyId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public LocalDate getTradeDate() { return tradeDate; }
    public TradeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public BigDecimal getNotional() {
        if (quantity == null || price == null) return null;
        return quantity.multiply(price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trade)) return false;
        Trade trade = (Trade) o;
        return Objects.equals(tradeRef, trade.tradeRef);
    }

    @Override
    public int hashCode() { return Objects.hashCode(tradeRef); }

    @Override
    public String toString() {
        return String.format("Trade[%s | %s | %s @ %s | %s | %s]",
                tradeRef,
                instrumentId != null ? instrumentId.toString() : "-",
                quantity != null ? quantity.toPlainString() : "-",
                price != null ? price.toPlainString() : "-",
                tradeDate != null ? tradeDate.toString() : "-",
                status != null ? status.name() : "-");
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

        public Builder tradeRef(String v) { this.tradeRef = v; return this; }
        public Builder instrumentId(Long v) { this.instrumentId = v; return this; }
        public Builder counterpartyId(Long v) { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v) { this.quantity = v; return this; }
        public Builder price(BigDecimal v) { this.price = v; return this; }
        public Builder tradeDate(LocalDate v) { this.tradeDate = v; return this; }
        public Builder status(TradeStatus v) { this.status = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        public Trade build() {
            Objects.requireNonNull(tradeRef, "tradeRef required");
            Objects.requireNonNull(quantity, "quantity required");
            Objects.requireNonNull(price, "price required");
            if (quantity.signum() <= 0) throw new IllegalStateException("quantity must be > 0");
            if (price.signum() <= 0) throw new IllegalStateException("price must be > 0");
            return new Trade(this);
        }
    }
}
