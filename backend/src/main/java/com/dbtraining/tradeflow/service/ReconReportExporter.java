package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.ReconResult;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * ============================================================================
 * ReconReportExporter — TICKET-I037
 * ============================================================================
 * WHAT:    Writes reconciliation results to a CSV file.
 * HOW:     Vanilla java.nio.file + BufferedWriter. NO external CSV library
 *          — the point is to learn quoting/escaping by hand.
 * WHY:     Ops users download daily recon CSVs to feed downstream systems.
 * OBSERVE: Field values containing `,` or `"` are quoted; embedded `"` is
 *          doubled to `""`.
 * ============================================================================
 *  TODO(TICKET-I037):
 *    public void exportReconReport(List<ReconResult> results, Path target)
 *
 *  HINTS:
 *    1. Write to a `.tmp` sibling file, then Files.move(..., ATOMIC_MOVE) —
 *       no half-written file is ever visible to a reader.
 *    2. Use try-with-resources for the writer.
 *    3. Header row: "trade_id,status,discrepancy_type,resolved_at"
 *    4. Helper: private String escape(String value) — handles comma, quote, newline.
 * ============================================================================
 */
@Service
public class ReconReportExporter {

    private static final String HEADER = "trade_ref,status,discrepancy_type,resolved_at";

    public void exportReconReport(List<ReconResult> results, Path target) throws IOException {
        Objects.requireNonNull(results, "results required");
        Objects.requireNonNull(target,  "target required");

        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        if (tmp.getParent() != null) Files.createDirectories(tmp.getParent());

        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();
            for (ReconResult r : results) {
                w.write(rowFor(r));
                w.newLine();
            }
            w.flush();
        }

        try {
            Files.move(tmp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailed) {
            // Cross-device or filesystem without atomic-move support: fall back to a plain replace.
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String rowFor(ReconResult r) {
        String tradeRef       = r.getTrade() != null && r.getTrade().getTradeRef() != null ? r.getTrade().getTradeRef() : "";
        String status         = r.getStatus() != null ? r.getStatus().name() : "";
        String discrepancy    = r.getDiscrepancyType() != null ? r.getDiscrepancyType().name() : "";
        Instant resolved      = r.getResolvedAt();
        String resolvedStr    = resolved != null ? resolved.toString() : "";
        return String.join(",",
                escape(tradeRef),
                escape(status),
                escape(discrepancy),
                escape(resolvedStr));
    }

    /** RFC-4180-style CSV escaping. Wraps in quotes when needed; doubles embedded quotes. */
    private String escape(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needsQuoting) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
