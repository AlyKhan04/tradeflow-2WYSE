package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Trade — generic trade type that shares the common BaseTrade fields.
 * Immutable from the outside: construction goes through the Builder.
 */@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_ref", nullable = false, unique = true, length = 30)
    private String tradeRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counterparty_id")
    private Counterparty counterparty;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** No-arg constructor required by JPA. */
    protected Trade() {}

    private Trade(Builder b) {
        this.tradeRef     = b.tradeRef;
        this.instrument   = b.instrument;
        this.counterparty = b.counterparty;
        this.quantity     = b.quantity;
        this.price        = b.price;
        this.tradeDate    = b.tradeDate;
        this.status       = b.status != null ? b.status : TradeStatus.PENDING;
        this.createdAt    = b.createdAt != null ? b.createdAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tradeRef;
        private Instrument instrument;
        private Counterparty counterparty;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;

        public Builder tradeRef(String v)          { this.tradeRef = v; return this; }
        public Builder instrument(Instrument v)    { this.instrument = v; return this; }
        public Builder counterparty(Counterparty v){ this.counterparty = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v; return this; }
        public Builder price(BigDecimal v)         { this.price = v; return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v; return this; }
        public Builder status(TradeStatus v)       { this.status = v; return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v; return this; }

        public Trade build() { return new Trade(this); }
    }

    public Long getId()                  { return id; }
    public String getTradeRef()          { return tradeRef; }
    public Instrument getInstrument()    { return instrument; }
    public Counterparty getCounterparty(){ return counterparty; }
    public BigDecimal getQuantity()      { return quantity; }
    public BigDecimal getPrice()         { return price; }
    public LocalDate getTradeDate()      { return tradeDate; }
    public TradeStatus getStatus()       { return status; }
    public Instant getCreatedAt()        { return createdAt; }

    public void setStatus(TradeStatus status) { this.status = status; }

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
    public int hashCode() { return Objects.hash(tradeRef); }
}