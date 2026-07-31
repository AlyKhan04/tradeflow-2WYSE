package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconResultDto;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.repository.ReconResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private final ReconResultRepository reconResultRepository;

    public ReconciliationService(ReconResultRepository reconResultRepository) {
        this.reconResultRepository = reconResultRepository;
    }

    public ReconciliationService() {
        this.reconResultRepository = null;
    }

    @Transactional(readOnly = true)
    public ReconSummary runForAll() {
        if (reconResultRepository == null) {
            return new ReconSummary(0, 0, 0, 0, Map.of());
        }
        long matched = reconResultRepository.countByStatus(ReconResult.Status.RESOLVED);
        long unmatched = reconResultRepository.countByStatus(ReconResult.Status.OPEN);

        Map<DiscrepancyType, Integer> breakdown = new EnumMap<>(DiscrepancyType.class);
        for (DiscrepancyType t : DiscrepancyType.values()) {
            breakdown.put(t, 0);
        }
        for (ReconResult r : reconResultRepository.findByStatus(ReconResult.Status.OPEN)) {
            breakdown.merge(r.getDiscrepancyType(), 1, Integer::sum);
        }

        long total = matched + unmatched;
        return new ReconSummary((int) total, (int) total, (int) matched, (int) unmatched, breakdown);
    }

    @Transactional(readOnly = true)
    public Page<ReconResultDto> listBreaks(ReconResult.Status status,
                                           Long counterpartyId,
                                           Pageable pageable) {
        if (reconResultRepository == null) {
            return Page.empty();
        }
        Page<ReconResult> page = (counterpartyId == null)
                ? reconResultRepository.findByStatus(status, pageable)
                : reconResultRepository.findByStatusAndCounterpartyId(status, counterpartyId, pageable);
        return page.map(ReconResultDto::from);
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
}
