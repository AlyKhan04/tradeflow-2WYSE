package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * ReconResult — JPA Entity recording one reconciliation break against one Trade.
 */
@Entity
@Table(name = "recon_breaks")
public class ReconResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "status", nullable = false)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type")
    private DiscrepancyType discrepancyType;

    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public ReconResult() {}

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
