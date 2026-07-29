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
 *
 * HINTS:
 * - Use BigDecimal for `quantity` + `price` (NEVER double — it loses precision
 *   for money).
 * - Use LocalDate (NOT Date) for tradeDate.
 * - Use Instant (NOT Date) for createdAt.
 * - For JPA: a `protected Trade()` no-arg constructor satisfies Hibernate;
 *   the public path stays via the Builder.
 * - For @ManyToOne on instrument/counterparty: use FetchType.LAZY to avoid
 *   accidental N+1 queries.
 * ============================================================================
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

    Trade() {}

    private Trade(Builder b) {
        this.tradeRef       = b.tradeRef;
        this.instrumentId   = b.instrumentId;
        this.counterpartyId = b.counterpartyId;
        this.quantity       = b.quantity;
        this.price          = b.price;
        this.tradeDate      = b.tradeDate;
        this.status         = b.status != null ? b.status : TradeStatus.PENDING;
        this.createdAt      = b.createdAt != null ? b.createdAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    // ------------------------------------------------------------------------
    // TICKET-I017 — getters only, no setters. Construction goes through the
    // Builder below, so a Trade cannot be mutated after it is built.
    // ------------------------------------------------------------------------
    public String getTradeRef()     { return tradeRef; }
    public Long getInstrumentId()   { return instrumentId; }
    public Long getCounterpartyId() { return counterpartyId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice()    { return price; }
    public LocalDate getTradeDate() { return tradeDate; }
    public TradeStatus getStatus()  { return status; }
    public Instant getCreatedAt()   { return createdAt; }

    /** Notional = quantity * price. Computed on demand, never stored. */
    public BigDecimal getNotional() {
        return quantity == null || price == null ? null : quantity.multiply(price);
    }

    // ------------------------------------------------------------------------
    // TICKET-I025 — equality on tradeRef, the business key.
    //
    // Two feeds publishing TRD-2026-0001 describe the same trade even when
    // their price, quantity and timestamps differ, so tradeRef alone decides
    // identity. Day 3's reconciliation dedups via HashMap<String, Trade> on
    // exactly this key.
    //
    // hashCode() must stay consistent with equals(): a HashMap jumps to a
    // bucket by hash *before* it ever calls equals(), so if two equal Trades
    // hashed differently the comparison would never happen and lookups would
    // silently miss.
    // ------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Pattern variable (Java 16+): type-test and bind in one step.
        // Also returns false for null, so no explicit null check is needed.
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