package com.dbtraining.tradeflow.model;

import java.time.Instant;
import java.util.Objects;

/**
 * ============================================================================
 * ReconResult — TICKET-I024 + TICKET-I058
 * ============================================================================
 * WHAT:    Outcome of comparing one trade against its external counterpart.
 *          One row per break (or per matched trade, depending on team policy).
 * HOW:     POJO on Day 2; @Entity on Day 5.
 * WHY:     The Ops UI page on Day 8 lists ReconResults so users can resolve.
 * OBSERVE: A row with status='OPEN' and discrepancyType=PRICE_MISMATCH means
 *          a human has to investigate.
 * ============================================================================
 */

// backend/src/main/java/com/dbtraining/tradeflow/model/ReconResult.java
package com.dbtraining.tradeflow.model;

import java.time.Instant;
import java.util.Objects;

/**
 * ReconResult — POJO recording one reconciliation break against one Trade.
 * status is a String for Day 2; Day 5 promotes it to a ReconStatus enum.
 */
public class ReconResult {

    private final Long id;
    private final Long tradeId;
    private final String status;
    private final DiscrepancyType discrepancyType;
    private final Instant detectedAt;
    private final Instant resolvedAt;
    private final Instant createdAt;

    private ReconResult(Builder builder) {
        this.id = builder.id;
        this.tradeId = builder.tradeId;
        this.status = Objects.requireNonNull(builder.status, "status is required");
        this.discrepancyType = builder.discrepancyType;
        this.detectedAt = Objects.requireNonNull(builder.detectedAt, "detectedAt is required");
        this.resolvedAt = builder.resolvedAt;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt is required");
    }

    public Long getId() {
        return id;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public String getStatus() {
        return status;
    }

    public DiscrepancyType getDiscrepancyType() {
        return discrepancyType;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private Long tradeId;
        private String status;
        private DiscrepancyType discrepancyType;
        private Instant detectedAt;
        private Instant resolvedAt;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder tradeId(Long tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder discrepancyType(DiscrepancyType discrepancyType) {
            this.discrepancyType = discrepancyType;
            return this;
        }

        public Builder detectedAt(Instant detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }

        public Builder resolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ReconResult build() {
            return new ReconResult(this);
        }
    }
}
