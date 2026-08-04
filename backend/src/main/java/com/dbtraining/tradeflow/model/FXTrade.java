package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;

/**
 * ============================================================================
 * FXTrade — TICKET-I030
 * ============================================================================
 * WHAT:    A foreign-exchange trade (EUR/USD, GBP/JPY, ...).
 * WHY:     FX has no exchange — pricing is OTC and settled bilaterally.
 *          We track the currency pair + spot rate explicitly.
 * ============================================================================
 *  TODO(TICKET-I030):
 *    extends BaseTrade.
 *    Extra fields:
 *      private final String baseCurrency;    // "EUR" in EUR/USD
 *      private final String quoteCurrency;   // "USD" in EUR/USD
 *      private final BigDecimal spotRate;
 *
 *    Validation in builder: baseCurrency != quoteCurrency.
 *    assetClassDescription() returns baseCurrency + "/" + quoteCurrency.
 * ============================================================================
 */
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class FXTrade extends BaseTrade {

    private final String baseCurrency;
    private final String quoteCurrency;
    private final BigDecimal spotRate;

    private FXTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        this.baseCurrency  = Objects.requireNonNull(b.baseCurrency,  "baseCurrency required");
        this.quoteCurrency = Objects.requireNonNull(b.quoteCurrency, "quoteCurrency required");
        this.spotRate      = Objects.requireNonNull(b.spotRate,      "spotRate required");
        if (baseCurrency.length() != 3 || quoteCurrency.length() != 3)
            throw new IllegalStateException("currencies must be ISO-4217 3-letter codes");
        if (baseCurrency.equals(quoteCurrency))
            throw new IllegalStateException("base and quote currencies must differ");
        if (spotRate.signum() <= 0)
            throw new IllegalStateException("spotRate must be > 0");
    }

    public static Builder builder() { return new Builder(); }

    public String getBaseCurrency()  { return baseCurrency; }
    public String getQuoteCurrency() { return quoteCurrency; }
    public BigDecimal getSpotRate()  { return spotRate; }

    @Override
    public String assetClassDescription() { return baseCurrency + "/" + quoteCurrency; }

    public static final class Builder {
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;
        private String baseCurrency;
        private String quoteCurrency;
        private BigDecimal spotRate;

        public Builder tradeRef(String v)         { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)        { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)      { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)         { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)       { this.status = v;         return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v;      return this; }
        public Builder baseCurrency(String v)      { this.baseCurrency = v;   return this; }
        public Builder quoteCurrency(String v)     { this.quoteCurrency = v;  return this; }
        public Builder spotRate(BigDecimal v)      { this.spotRate = v;       return this; }

        public FXTrade build() { return new FXTrade(this); }
    }
}
