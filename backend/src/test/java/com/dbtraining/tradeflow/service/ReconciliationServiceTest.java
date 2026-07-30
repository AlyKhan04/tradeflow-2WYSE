package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private TradeDAO tradeDAO;

    @Mock
    private ReconResultDAO reconResultDAO;

    private ReconciliationService service;
    private ReconciliationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService();
        orchestrator = new ReconciliationOrchestrator(tradeDAO, reconResultDAO, service);
    }

    @Test
    @DisplayName("Day 3: matchTrades with matching trades returns 0 discrepancies")
    void matchTrades_allMatched_returnsEmptyDiscrepancies() {
        List<BaseTrade> internal = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));
        List<BaseTrade> external = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.matched()).hasSize(3);
        assertThat(report.totalInternal()).isEqualTo(3);
        assertThat(report.totalExternal()).isEqualTo(3);
    }

    @Test
    @DisplayName("Day 3: matchTrades with price difference flags PRICE_MISMATCH")
    void matchTrades_priceMismatch_flagsDiscrepancy() {
        BaseTrade in = equityWith("TRD-001", new BigDecimal("100"), new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));
        BaseTrade out = equityWith("TRD-001", new BigDecimal("100"), new BigDecimal("249.99"), LocalDate.of(2026, 3, 1));

        ReconReport report = service.matchTrades(List.of(in), List.of(out));

        assertThat(report.matched()).isEmpty();
        assertThat(report.discrepancies()).hasSize(1);
        Discrepancy discrepancy = report.discrepancies().get(0);
        assertThat(discrepancy.tradeRef()).isEqualTo("TRD-001");
        assertThat(discrepancy.types()).containsExactly(DiscrepancyType.PRICE_MISMATCH);
    }

    @Test
    void matchTrades_priceScaleDifference_notFlagged() {
        BaseTrade in = equityWith("TRD-002", new BigDecimal("100"), new BigDecimal("245.5"), LocalDate.of(2026, 3, 1));
        BaseTrade out = equityWith("TRD-002", new BigDecimal("100"), new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));

        ReconReport report = service.matchTrades(List.of(in), List.of(out));

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.matched()).hasSize(1);
    }

    @Test
    @DisplayName("Day 3: matchTrades with missing external trade flags MISSING_TRADE")
    void matchTrades_missingExternal_flagsMissingTrade() {
        List<BaseTrade> internal = List.of(equity("TRD-INT-ONLY"));
        List<BaseTrade> external = List.of();

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).hasSize(1);
        assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-INT-ONLY");
        assertThat(report.discrepancies().get(0).types()).containsExactly(DiscrepancyType.MISSING_TRADE);
    }

    @Test
    void matchTrades_missingInternal_flagsMissingTrade() {
        List<BaseTrade> internal = List.of();
        List<BaseTrade> external = List.of(equity("TRD-EXT-ONLY"));

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).hasSize(1);
        assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-EXT-ONLY");
        assertThat(report.discrepancies().get(0).types()).containsExactly(DiscrepancyType.MISSING_TRADE);
    }

    @Test
    void mockedTradeDAO_findAllCalledOnce() {
        List<Trade> sample = List.of(sampleTrade("TRD-1"));
        when(tradeDAO.findAll()).thenReturn(sample);

        ReconSummary summary = orchestrator.runForAll();

        verify(tradeDAO, times(1)).findAll();
        assertThat(summary.totalInternal()).isEqualTo(1);
    }

    @Test
    void runForAll_oneDiscrepancy_insertsOneReconResult() {
        List<Trade> sample = List.of(sampleTrade("TRD-INT-ONLY"));
        when(tradeDAO.findAll()).thenReturn(sample);
        orchestrator.withExternalTrades(List.of());

        orchestrator.runForAll();

        ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
        verify(reconResultDAO, times(1)).insert(captor.capture());
        ReconResult inserted = captor.getValue();
        assertThat(inserted.getDiscrepancyType()).isEqualTo(DiscrepancyType.MISSING_TRADE);
        assertThat(inserted.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void runForAll_allMatched_neverCallsInsert() {
        List<Trade> sample = List.of(sampleTrade("TRD-1"));
        when(tradeDAO.findAll()).thenReturn(sample);
        orchestrator.withExternalTrades(List.of(equity("TRD-1")));

        orchestrator.runForAll();

        verify(reconResultDAO, never()).insert(any(ReconResult.class));
    }

    private static BaseTrade equity(String tradeRef) {
        return EquityTrade.builder()
                .tradeRef(tradeRef)
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("10.00"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA")
                .lotSize(100)
                .build();
    }

    private static BaseTrade equityWith(String tradeRef, BigDecimal qty, BigDecimal price, LocalDate date) {
        return EquityTrade.builder()
                .tradeRef(tradeRef)
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(qty)
                .price(price)
                .tradeDate(date)
                .status(TradeStatus.MATCHED)
                .exchange("XETRA")
                .lotSize(100)
                .build();
    }

    private static Trade sampleTrade(String tradeRef) {
        return Trade.builder()
                .tradeRef(tradeRef)
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("10.00"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .build();
    }

    private static final class ReconciliationOrchestrator {
        private final TradeDAO tradeDAO;
        private final ReconResultDAO reconResultDAO;
        private final ReconciliationService service;
        private List<BaseTrade> externalTrades = List.of();

        private ReconciliationOrchestrator(TradeDAO tradeDAO, ReconResultDAO reconResultDAO, ReconciliationService service) {
            this.tradeDAO = tradeDAO;
            this.reconResultDAO = reconResultDAO;
            this.service = service;
        }

        private ReconciliationOrchestrator withExternalTrades(List<BaseTrade> externalTrades) {
            this.externalTrades = externalTrades;
            return this;
        }

        private ReconSummary runForAll() {
            List<Trade> internalTrades = tradeDAO.findAll();
            List<BaseTrade> internal = new java.util.ArrayList<>(internalTrades.size());
            internal.addAll(internalTrades);
            ReconReport report = service.matchTrades(internal, externalTrades);
            for (Discrepancy discrepancy : report.discrepancies()) {
                ReconResult result = ReconResult.builder()
                        .tradeId(1L)
                        .status("OPEN")
                        .discrepancyType(discrepancy.types().get(0))
                        .build();
                reconResultDAO.insert(result);
            }
            return service.generateReport(report);
        }
    }
}
