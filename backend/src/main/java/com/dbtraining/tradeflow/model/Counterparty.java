package com.dbtraining.tradeflow.model;

import java.util.Objects;

/**
 * ============================================================================
 * Counterparty — TICKET-I022 + TICKET-I057
 * ============================================================================
 * WHAT:    The other side of a trade — broker, exchange, or another bank.
 * HOW:     Plain POJO on Day 2; converted to JPA @Entity on Day 5.
 * WHY:     The trade table has an FK to this — every trade has a counterparty.
 * OBSERVE: Build a Counterparty in main(), pass it through a Trade, print both.
 * ============================================================================
 *  TODO(TICKET-I022) [Day 2]:
 *    Fields: id (Long), name (String), leiCode (String, 20 chars), region (String).
 *    Add private constructor + Builder for clean construction.
 *    Add equals()/hashCode() on leiCode (it's globally unique).
 *
 *  TODO(TICKET-I057) [Day 5]:
 *    Convert this class to a JPA entity.
 *    - @Entity @Table(name = "counterparties")
 *    - @Id @GeneratedValue(strategy = GenerationType.IDENTITY) on id
 *    - @Column(unique = true, length = 20) on leiCode
 *    - protected no-arg constructor (JPA needs it).
 * ============================================================================
 */
public class Counterparty {

    private Long id;
    private String name;
    private String leiCode;
    private String region;

    Counterparty() {}

    private Counterparty(Builder b) {
        this.name    = b.name;
        this.leiCode = b.leiCode;
        this.region  = b.region;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId()        { return id; }
    public String getName()    { return name; }
    public String getLeiCode() { return leiCode; }
    public String getRegion()  { return region; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Counterparty other)) return false;
        return Objects.equals(leiCode, other.leiCode);
    }

    @Override public int hashCode() { return Objects.hash(leiCode); }

    @Override public String toString() {
        return "Counterparty[" + leiCode + " | " + name + " | " + region + "]";
    }

    public static final class Builder {
        private String name;
        private String leiCode;
        private String region;

        public Builder name(String v)    { this.name = v;    return this; }
        public Builder leiCode(String v) { this.leiCode = v; return this; }
        public Builder region(String v)  { this.region = v;  return this; }

        public Counterparty build() {
            Objects.requireNonNull(name,    "name required");
            Objects.requireNonNull(leiCode, "leiCode required");
            Objects.requireNonNull(region,  "region required");
            if (leiCode.length() != 20)
                throw new IllegalStateException("leiCode must be exactly 20 chars (LEI standard)");
            if (!region.matches("APAC|EMEA|NAMR|LATAM"))
                throw new IllegalStateException("region must be one of APAC|EMEA|NAMR|LATAM");
            return new Counterparty(this);
        }
    }
}
