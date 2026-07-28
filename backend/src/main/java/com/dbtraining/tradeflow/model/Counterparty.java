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
 */
public class Counterparty {

    private final Long id;
    private final String name;
    private final String leiCode;
    private final String region;

    private Counterparty(Builder builder) {
        this.id = builder.id;
        this.name = Objects.requireNonNull(builder.name, "name is required");
        this.leiCode = Objects.requireNonNull(builder.leiCode, "leiCode is required");
        this.region = Objects.requireNonNull(builder.region, "region is required");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLeiCode() {
        return leiCode;
    }

    public String getRegion() {
        return region;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Counterparty that)) return false;
        return Objects.equals(leiCode, that.leiCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leiCode);
    }

    @Override
    public String toString() {
        return String.format("Counterparty[%s / %s / %s]", name, leiCode, region);
    }

    public static final class Builder {
        private Long id;
        private String name;
        private String leiCode;
        private String region;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder leiCode(String leiCode) {
            this.leiCode = leiCode;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Counterparty build() {
            return new Counterparty(this);
        }
    }
}
