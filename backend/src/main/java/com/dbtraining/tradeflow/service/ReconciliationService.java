package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.Discrepancy;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
public class ReconciliationService {

    private final TradeDAO tradeDAO;
    private final ReconResultDAO reconResultDAO;

    public ReconciliationService(TradeDAO tradeDAO, ReconResultDAO reconResultDAO) {
        this.tradeDAO = tradeDAO;
        this.reconResultDAO = reconResultDAO;
    }

    public ReconReport matchTrades(List<? extends BaseTrade> internal, List<? extends BaseTrade> external) {
        List<? extends BaseTrade> internalTrades = internal == null ? List.of() : internal;
        List<? extends BaseTrade> externalTrades = external == null ? List.of() : external;

        Map<String, BaseTrade> externalByRef = externalTrades.stream()
                .filter(t -> t.getTradeRef() != null)
                .collect(Collectors.toMap(BaseTrade::getTradeRef, t -> t, (first, second) -> first));

        List<BaseTrade> matched = new ArrayList<>();
        List<Discrepancy> discrepancies = new ArrayList<>();

        for (BaseTrade internalTrade : internalTrades) {
            BaseTrade externalTrade = externalByRef.remove(internalTrade.getTradeRef());
            if (externalTrade == null) {
                discrepancies.add(new Discrepancy(internalTrade.getTradeRef(), List.of(DiscrepancyType.MISSING_TRADE)));
                continue;
            }
            List<DiscrepancyType> types = compareTrades(internalTrade, externalTrade);
            if (types.isEmpty()) {
                matched.add(internalTrade);
            } else {
                discrepancies.add(new Discrepancy(internalTrade.getTradeRef(), types));
            }
        }

        for (BaseTrade leftover : externalByRef.values()) {
            discrepancies.add(new Discrepancy(leftover.getTradeRef(), List.of(DiscrepancyType.MISSING_TRADE)));
        }

        return new ReconReport(matched, discrepancies, internalTrades.size(), externalTrades.size());
    }

    public ReconSummary generateReport(ReconReport reconReport) {
        Map<DiscrepancyType, Integer> breakdown = reconReport.discrepancies().stream()
                .flatMap(d -> d.types().stream())
                .collect(Collectors.toMap(type -> type, type -> 1, Integer::sum));

        return new ReconSummary(
                reconReport.totalInternal(),
                reconReport.totalExternal(),
                reconReport.matched().size(),
                reconReport.discrepancies().size(),
                breakdown);
    }

    public ReconSummary reconcileWithDatabase(List<? extends BaseTrade> externalTrades) {
        List<? extends BaseTrade> internalTrades = tradeDAO.findAll();
        ReconReport report = matchTrades(internalTrades, externalTrades);
        persistBreaks(report);
        return generateReport(report);
    }

    private void persistBreaks(ReconReport report) {
        for (Discrepancy discrepancy : report.discrepancies()) {
            ReconResult result = ReconResult.builder()
                    .tradeId(null)
                    .status("OPEN")
                    .discrepancyType(discrepancy.types().get(0))
                    .detectedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            reconResultDAO.insert(result);
        }
    }

    private static List<DiscrepancyType> compareTrades(BaseTrade internal, BaseTrade external) {
        List<DiscrepancyType> types = new ArrayList<>();
        if (internal.getQuantity().compareTo(external.getQuantity()) != 0) {
            types.add(DiscrepancyType.QUANTITY_MISMATCH);
        }
        if (internal.getPrice().compareTo(external.getPrice()) != 0) {
            types.add(DiscrepancyType.PRICE_MISMATCH);
        }
        if (!internal.getTradeDate().equals(external.getTradeDate())) {
            types.add(DiscrepancyType.DATE_MISMATCH);
        }
        return types;
    }
}
