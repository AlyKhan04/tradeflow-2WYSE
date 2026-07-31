package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;

import java.time.Instant;

/**
 * ReconResultDto — Outbound DTO for reconciliation breaks.
 */
public record ReconResultDto(
        Long id,
        Long tradeId,
        String tradeRef,
        DiscrepancyType discrepancyType,
        ReconResult.Status status,
        Instant detectedAt,
        Instant resolvedAt
) {
    public static ReconResultDto from(ReconResult entity) {
        return new ReconResultDto(
                entity.getId(),
                entity.getTrade() != null ? entity.getTrade().getId() : null,
                entity.getTrade() != null ? entity.getTrade().getTradeRef() : null,
                entity.getDiscrepancyType(),
                entity.getStatus(),
                entity.getDetectedAt(),
                entity.getResolvedAt()
        );
    }
}
