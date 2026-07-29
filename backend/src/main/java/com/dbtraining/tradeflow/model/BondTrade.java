package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ============================================================================
 * BondTrade — TICKET-I031
 * ============================================================================
 * WHAT:    A trade on a fixed-income instrument.
 * WHY:     Bonds need coupon and maturity to compute yield / settlement.
 * ============================================================================
 *  TODO(TICKET-I031):
 *    extends BaseTrade.
 *    Extra fields:
 *      private final BigDecimal couponRate;       // 0.0 .. 100.0
 *      private final LocalDate maturityDate;      // must be after tradeDate
 *      private final BigDecimal faceValue;
 *
 *    Validate in builder:
 *      - couponRate >= 0 && couponRate <= 100
 *      - maturityDate.isAfter(tradeDate)
 *
 *    assetClassDescription() returns
 *      "Bond coupon " + couponRate + "% mat " + maturityDate.
 * ============================================================================
 */
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class BondTrade extends BaseTrade {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal couponRate;
    private final LocalDate maturityDate;
    private final BigDecimal faceValue;

    private BondTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        this.couponRate   = Objects.requireNonNull(b.couponRate,   "couponRate required");
        this.maturityDate = Objects.requireNonNull(b.maturityDate, "maturityDate required");
        this.faceValue    = Objects.requireNonNull(b.faceValue,    "faceValue required");
        if (couponRate.signum() < 0 || couponRate.compareTo(HUNDRED) > 0)
            throw new IllegalStateException("couponRate must be between 0 and 100");
        if (!maturityDate.isAfter(tradeDate))
            throw new IllegalStateException("maturityDate must be after tradeDate");
        if (faceValue.signum() <= 0)
            throw new IllegalStateException("faceValue must be > 0");
    }

    public static Builder builder() { return new Builder(); }

    public BigDecimal getCouponRate()    { return couponRate; }
    public LocalDate getMaturityDate()   { return maturityDate; }
    public BigDecimal getFaceValue()     { return faceValue; }

    @Override
    public String assetClassDescription() {
        return "Bond coupon " + couponRate + "% mat " + maturityDate;
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
        private BigDecimal couponRate;
        private LocalDate maturityDate;
        private BigDecimal faceValue;

        public Builder tradeRef(String v)         { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)        { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)      { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)         { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)       { this.status = v;         return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v;      return this; }
        public Builder couponRate(BigDecimal v)    { this.couponRate = v;     return this; }
        public Builder maturityDate(LocalDate v)   { this.maturityDate = v;   return this; }
        public Builder faceValue(BigDecimal v)     { this.faceValue = v;      return this; }

        public BondTrade build() { return new BondTrade(this); }
    }
}
