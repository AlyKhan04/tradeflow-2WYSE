package com.dbtraining.tradeflow.model;

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
 *  TODO(TICKET-I024) [Day 2]:
 *    Fields: id, tradeId (Long), status (String for now), discrepancyType
 *            (DiscrepancyType, nullable), resolvedAt (Instant, nullable),
 *            createdAt (Instant).
 *
 *  TODO(TICKET-I058) [Day 5]:
 *    Convert to JPA entity.
 *    - @ManyToOne(fetch = LAZY) on the Trade reference
 *    - @Enumerated(EnumType.STRING) on discrepancyType
 *    - resolvedAt is @Column(nullable = true)
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

    private Long id;
    private Long tradeId;
    private String status;
    private DiscrepancyType discrepancyType;
    private Instant detectedAt;
    private Instant resolvedAt;

    ReconResult() {}

    private ReconResult(Builder b) {
        this.tradeId         = b.tradeId;
        this.discrepancyType = b.discrepancyType;
        this.status          = b.status != null ? b.status : "OPEN";
        this.detectedAt      = b.detectedAt != null ? b.detectedAt : Instant.now();
        this.resolvedAt      = b.resolvedAt;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId()                        { return id; }
    public Long getTradeId()                   { return tradeId; }
    public String getStatus()                  { return status; }
    public DiscrepancyType getDiscrepancyType(){ return discrepancyType; }
    public Instant getDetectedAt()             { return detectedAt; }
    public Instant getResolvedAt()             { return resolvedAt; }

    /** Mark this break resolved; sets resolvedAt = now. Idempotent. */
    public void resolve() {
        if ("RESOLVED".equals(this.status)) return;
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
    }

    public boolean isOpen() { return "OPEN".equals(status); }

    @Override public String toString() {
        return "ReconResult[trade=" + tradeId + " | " + discrepancyType + " | " + status + "]";
    }

    public static final class Builder {
        private Long tradeId;
        private String status;
        private DiscrepancyType discrepancyType;
        private Instant detectedAt;
        private Instant resolvedAt;

        public Builder tradeId(Long v)                      { this.tradeId = v;         return this; }
        public Builder status(String v)                     { this.status = v;          return this; }
        public Builder discrepancyType(DiscrepancyType v)   { this.discrepancyType = v; return this; }
        public Builder detectedAt(Instant v)                { this.detectedAt = v;      return this; }
        public Builder resolvedAt(Instant v)                { this.resolvedAt = v;      return this; }

        public ReconResult build() {
            Objects.requireNonNull(tradeId,         "tradeId required");
            Objects.requireNonNull(discrepancyType, "discrepancyType required");
            return new ReconResult(this);
        }
    }
}
