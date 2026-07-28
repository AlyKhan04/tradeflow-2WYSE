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
import java.time.Instant;
import java.util.Objects;

public class ReconResult {

    private final Long id;
    private final Long tradeId;
    private final String status;
    private final DiscrepancyType discrepancyType;
    private final Instant resolvedAt;
    private final Instant createdAt;

    protected ReconResult() {
        this.id = null;
        this.tradeId = null;
        this.status = null;
        this.discrepancyType = null;
        this.resolvedAt = null;
        this.createdAt = null;
    }

    private ReconResult(Builder b) {
        this.id = b.id;
        this.tradeId = b.tradeId;
        this.status = b.status;
        this.discrepancyType = b.discrepancyType;
        this.resolvedAt = b.resolvedAt;
        this.createdAt = b.createdAt != null ? b.createdAt : Instant.now();
    }

    public Long getId() { return id; }
    public Long getTradeId() { return tradeId; }
    public String getStatus() { return status; }
    public DiscrepancyType getDiscrepancyType() { return discrepancyType; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReconResult)) return false;
        ReconResult that = (ReconResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return String.format("ReconResult[id=%s tradeId=%s status=%s discrepancy=%s]",
                id != null ? id.toString() : "-",
                tradeId != null ? tradeId.toString() : "-",
                status != null ? status : "-",
                discrepancyType != null ? discrepancyType.name() : "-");
    }

    public static final class Builder {
        private Long id;
        private Long tradeId;
        private String status;
        private DiscrepancyType discrepancyType;
        private Instant resolvedAt;
        private Instant createdAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder tradeId(Long v) { this.tradeId = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder discrepancyType(DiscrepancyType v) { this.discrepancyType = v; return this; }
        public Builder resolvedAt(Instant v) { this.resolvedAt = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        public ReconResult build() {
            Objects.requireNonNull(status, "status required");
            Objects.requireNonNull(tradeId, "tradeId required");
            return new ReconResult(this);
        }
    }
}
