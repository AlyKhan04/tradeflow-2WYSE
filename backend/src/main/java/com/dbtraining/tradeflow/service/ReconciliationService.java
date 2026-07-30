package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.exception.TradeNotFoundException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.repository.ReconResultRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * ReconciliationService — TICKET-I034 + TICKET-I035 + TICKET-I036
 * ============================================================================
 * WHAT:    The heart of the system. Compares internal vs external trade lists,
 *          classifies discrepancies, persists results.
 * HOW:     Pure-Java on Day 3 (matchTrades + generateReport). On Day 5 this
 *          becomes a @Service injected with TradeRepository + ReconResultRepository.
 * WHY:     Single class with one job — find breaks. Easy to unit-test
 *          (Day 4 tests target this directly).
 * OBSERVE: Given the same input twice, the output is identical (pure function
 *          property — important for testability).
 *
 *  TICKET-I034: matchTrades(internal, external) -> ReconReport
 *  TICKET-I035: classify each pair: PRICE_MISMATCH / QUANTITY_MISMATCH /
 *               DATE_MISMATCH / MISSING_TRADE
 *  TICKET-I036: generateReport() -> ReconSummary
 * ============================================================================
 *
 * HINTS:
 *  - Build a Map<String, BaseTrade> externalByRef before the loop — O(1) lookup
 *    beats O(n²) nested iteration.
 *  - BigDecimal comparisons: NEVER `.equals()` (1.0 != 1.00). Use `compareTo() == 0`.
 *  - One trade can have multiple discrepancy types — your DTO must allow a List.
 *  - Keep this class < 200 lines. Pull helpers into private methods.
 * ============================================================================
 */
@Service
public class ReconciliationService {

    private final ReconResultRepository reconResultRepository;
    private final Counter reconResolvedCounter;
    private final Timer reconRunTimer;

    public ReconciliationService() {
        this(null, null);
    }

    public ReconciliationService(ReconResultRepository reconResultRepository, MeterRegistry registry) {
        this.reconRunTimer = (registry == null)
                ? null
                : Timer.builder("tradeflow_recon_run_seconds")
                .description("Time taken for a full reconciliation run")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);
        this.reconResultRepository = reconResultRepository;
        this.reconResolvedCounter = (registry == null)
                ? null
                : Counter.builder("tradeflow_recon_resolved_total")
                .description("Total reconciliation breaks resolved")
                .register(registry);
    }

    public ReconReport matchTrades(List<BaseTrade> internal, List<BaseTrade> external) {
        Objects.requireNonNull(internal, "internal list required");
        Objects.requireNonNull(external, "external list required");

        Map<String, BaseTrade> externalByRef = external.stream()
                .collect(Collectors.toMap(BaseTrade::getTradeRef, t -> t, (a, b) -> a));

        List<BaseTrade> matched = new ArrayList<>();
        List<Discrepancy> discrepancies = new ArrayList<>();

        for (BaseTrade in : internal) {
            BaseTrade out = externalByRef.remove(in.getTradeRef());
            if (out == null) {
                discrepancies.add(new Discrepancy(in.getTradeRef(), List.of(DiscrepancyType.MISSING_TRADE)));
                continue;
            }
            List<DiscrepancyType> diffs = classify(in, out);
            if (diffs.isEmpty()) {
                matched.add(in);
            } else {
                discrepancies.add(new Discrepancy(in.getTradeRef(), diffs));
            }
        }

        for (BaseTrade leftover : externalByRef.values()) {
            discrepancies.add(new Discrepancy(leftover.getTradeRef(),
                    List.of(DiscrepancyType.MISSING_TRADE)));
        }

        return new ReconReport(internal.size(), external.size(), matched, discrepancies);
    }

    private List<DiscrepancyType> classify(BaseTrade in, BaseTrade out) {
        List<DiscrepancyType> diffs = new ArrayList<>(2);
        if (in.getPrice().compareTo(out.getPrice()) != 0) {
            diffs.add(DiscrepancyType.PRICE_MISMATCH);
        }
        if (in.getQuantity().compareTo(out.getQuantity()) != 0) {
            diffs.add(DiscrepancyType.QUANTITY_MISMATCH);
        }
        if (!Objects.equals(in.getTradeDate(), out.getTradeDate())) {
            diffs.add(DiscrepancyType.DATE_MISMATCH);
        }
        return diffs;
    }

    public ReconSummary generateReport(ReconReport report) {
        Objects.requireNonNull(report, "report required");

        Map<DiscrepancyType, Integer> breakdown = new EnumMap<>(DiscrepancyType.class);
        for (DiscrepancyType t : DiscrepancyType.values()) {
            breakdown.put(t, 0);
        }
        for (Discrepancy d : report.discrepancies()) {
            for (DiscrepancyType t : d.types()) {
                breakdown.merge(t, 1, Integer::sum);
            }
        }
        return new ReconSummary(
                report.totalInternal(),
                report.totalExternal(),
                report.matched().size(),
                report.discrepancies().size(),
                Collections.unmodifiableMap(breakdown));
    }

    public String render(ReconSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reconciliation summary\n----------------------\n")
                .append(String.format("  Internal trades : %d%n", s.totalInternal()))
                .append(String.format("  External trades : %d%n", s.totalExternal()))
                .append(String.format("  Matched         : %d%n", s.matchedCount()))
                .append(String.format("  With breaks     : %d%n", s.unmatchedCount()))
                .append("  Breakdown:\n");
        s.breakdownByType().forEach((type, count) ->
                sb.append(String.format("    - %-20s %d%n", type, count)));
        return sb.toString();
    }

    @Transactional
    public void resolveBreak(Long id) {
        if (reconResultRepository == null) {
            throw new IllegalStateException("ReconResultRepository is not configured");
        }
        if (reconResolvedCounter == null) {
            throw new IllegalStateException("MeterRegistry is not configured");
        }

        ReconResult r = reconResultRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Recon break " + id + " not found"));
        if (r.getStatus() == ReconResult.Status.RESOLVED) {
            return;  // idempotent
        }
        r.resolve();
        reconResolvedCounter.increment();
        // Audit row is written by the Day-2 DB trigger on UPDATE.
    }
}
