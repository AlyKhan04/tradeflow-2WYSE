package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * EquityTrade — TICKET-I029
 * ============================================================================
 * WHAT:    A trade on an equity (shares of a listed company).
 * WHY:     Equities are exchange-listed, so we need the exchange name + lot size.
 * ============================================================================
 *  TODO(TICKET-I029):
 *    extends BaseTrade.
 *    Extra fields:
 *      private final String exchange;     // e.g. "XETRA", "NASDAQ"
 *      private final int lotSize;         // typical 100 for US, 1 for European
 *
 *    assetClassDescription() returns "Equity on " + exchange.
 * ============================================================================
 */
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class EquityTrade extends BaseTrade {

    private final String exchange;
    private final int lotSize;

    private EquityTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        this.exchange = Objects.requireNonNull(b.exchange, "exchange required");
        if (b.lotSize <= 0) throw new IllegalStateException("lotSize must be > 0");
        this.lotSize = b.lotSize;
    }

    public static Builder builder() { return new Builder(); }

    public String getExchange() { return exchange; }
    public int getLotSize()     { return lotSize; }

    @Override
    public String assetClassDescription() { return "Equity on " + exchange; }

    public static final class Builder {
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;
        private String exchange;
        private int lotSize;

        public Builder tradeRef(String v)         { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)        { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)      { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)         { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)       { this.status = v;         return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v;      return this; }
        public Builder exchange(String v)          { this.exchange = v;       return this; }
        public Builder lotSize(int v)              { this.lotSize = v;        return this; }

        public EquityTrade build() { return new EquityTrade(this); }
    }
}