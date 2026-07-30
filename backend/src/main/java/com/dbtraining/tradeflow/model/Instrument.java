package com.dbtraining.tradeflow.model;
import jakarta.persistence.*;
import java.util.Objects;
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
@Entity
@Table(name = "instruments")
public class Instrument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private AssetClass assetClass;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(unique = true, length = 12)
    private String isin;   // nullable — FX/commodities have no ISIN

    protected Instrument() {}

    private Instrument(Builder b) {
        this.symbol     = b.symbol;
        this.name       = b.name;
        this.assetClass = b.assetClass;
        this.currency   = b.currency;
        this.isin       = b.isin;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String symbol;
        private String name;
        private AssetClass assetClass;
        private String currency;
        private String isin;

        public Builder symbol(String v)         { this.symbol = v; return this; }
        public Builder name(String v)           { this.name = v; return this; }
        public Builder assetClass(AssetClass v) { this.assetClass = v; return this; }
        public Builder currency(String v)       { this.currency = v; return this; }
        public Builder isin(String v)           { this.isin = v; return this; }

        public Instrument build() { return new Instrument(this); }
    }

    public Long getId()              { return id; }
    public String getSymbol()        { return symbol; }
    public String getName()          { return name; }
    public AssetClass getAssetClass(){ return assetClass; }
    public String getCurrency()      { return currency; }
    public String getIsin()          { return isin; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instrument other)) return false;
        return Objects.equals(symbol, other.symbol);
    }
    @Override public int hashCode() { return Objects.hash(symbol); }
}