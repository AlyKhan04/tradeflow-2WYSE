package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * Instrument — TICKET-I023 + TICKET-I057
 * ============================================================================
 * WHAT:    What is being traded — equity, bond, FX pair, future, etc.
 * HOW:     POJO on Day 2; @Entity on Day 5.
 * WHY:     The trade table has an FK to this; the dashboard groups trades by
 *          instrument.
 * OBSERVE: Set assetClass = EQUITY, currency = "EUR", and the trade summary
 *          should treat it as a cash product.
 * ============================================================================
 *  TODO(TICKET-I023) [Day 2]:
 *    Fields: id (Long), symbol (String, e.g. "SAP.DE"), name (String),
 *            assetClass (AssetClass), currency (String length 3).
 *    Validate currency length in the Builder.
 *
 *  TODO(TICKET-I057) [Day 5]:
 *    Make this a JPA entity.
 *    - @Enumerated(EnumType.STRING) on assetClass
 *    - @Column(length = 3, nullable = false) on currency
 * ============================================================================
 */
import java.util.Objects;

public class Instrument {

    private final Long id;
    private final String symbol;
    private final String name;
    private final AssetClass assetClass;
    private final String currency;

    protected Instrument() {
        this.id = null;
        this.symbol = null;
        this.name = null;
        this.assetClass = null;
        this.currency = null;
    }

    private Instrument(Builder b) {
        this.id = b.id;
        this.symbol = b.symbol;
        this.name = b.name;
        this.assetClass = b.assetClass;
        this.currency = b.currency;
    }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public AssetClass getAssetClass() { return assetClass; }
    public String getCurrency() { return currency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instrument)) return false;
        Instrument that = (Instrument) o;
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() { return symbol != null ? symbol.hashCode() : 0; }

    @Override
    public String toString() {
        return String.format("Instrument[%s | %s | %s]", symbol != null ? symbol : "-", name != null ? name : "-", assetClass != null ? assetClass.name() : "-");
    }

    public static final class Builder {
        private Long id;
        private String symbol;
        private String name;
        private AssetClass assetClass;
        private String currency;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder symbol(String v) { this.symbol = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder assetClass(AssetClass v) { this.assetClass = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }

        public Instrument build() {
            Objects.requireNonNull(symbol, "symbol required");
            Objects.requireNonNull(assetClass, "assetClass required");
            Objects.requireNonNull(currency, "currency required");
            if (currency.length() != 3) throw new IllegalStateException("currency must be 3 characters (ISO)");
            this.currency = currency.toUpperCase();
            return new Instrument(this);
        }
    }
}
