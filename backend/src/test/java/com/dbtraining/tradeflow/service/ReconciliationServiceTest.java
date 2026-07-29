package com.dbtraining.tradeflow.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
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
        BaseTrade trade = equity("TRD-1");
        List<BaseTrade> internal = List.of(trade);
        List<BaseTrade> external = List.of(trade);

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.matched()).hasSize(1);
    }

    @Test
    @DisplayName("Day 3: matchTrades with price difference flags PRICE_MISMATCH")
    void matchTrades_priceMismatch_flagsDiscrepancy() {
        BaseTrade internal = equity("TRD-1");
        BaseTrade external = EquityTrade.builder()
                .tradeRef("TRD-1")
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("250.00"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETR")
                .lotSize(100)
                .build();

        ReconReport report = service.matchTrades(List.of(internal), List.of(external));

        assertThat(report.discrepancies()).hasSize(1);
        assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-1");
        assertThat(report.discrepancies().get(0).types()).containsExactly(DiscrepancyType.PRICE_MISMATCH);
    }

    @Test
    @DisplayName("Day 3: matchTrades with missing external trade flags MISSING_TRADE")
    void matchTrades_missingExternal_flagsMissingTrade() {
        List<BaseTrade> internal = List.of(equity("TRD-INT-ONLY"));
        List<BaseTrade> external = List.of();

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).hasSize(1);
        assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-INT-ONLY");
        assertThat(report.discrepancies().get(0).types())
                .containsExactly(DiscrepancyType.MISSING_TRADE);
    }

    @Test
    void matchTrades_missingInternal_flagsMissingTrade() {
        List<BaseTrade> internal = List.of();
        List<BaseTrade> external = List.of(equity("TRD-EXT-ONLY"));

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).hasSize(1);
        assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-EXT-ONLY");
        assertThat(report.discrepancies().get(0).types())
                .containsExactly(DiscrepancyType.MISSING_TRADE);
    }

    @Test
    void mockedTradeDAO_findAllCalledOnce() {
        Trade sample = Trade.builder()
                .tradeRef("TRD-1")
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .build();

        when(tradeDAO.findAll()).thenReturn(List.of(sample));

        ReconSummary summary = orchestrator.runForAll();

        verify(tradeDAO, times(1)).findAll();
        assertThat(summary.totalInternal()).isEqualTo(1);
        assertThat(summary.totalExternal()).isEqualTo(1);
        assertThat(summary.unmatchedCount()).isZero();
    }

    @Test
    void mockedReconResultDAO_insertCalledPerDiscrepancy() {
        Trade internalOnly = Trade.builder()
                .tradeRef("TRD-INT-ONLY")
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .build();

        when(tradeDAO.findAll()).thenReturn(List.of(internalOnly));

        orchestrator.runForAll(List.of());

        ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
        verify(reconResultDAO, times(1)).insert(captor.capture());
        assertThat(captor.getValue().getDiscrepancyType()).isEqualTo(DiscrepancyType.MISSING_TRADE);
        assertThat(captor.getValue().getStatus()).isEqualTo("OPEN");
    }

    private static BaseTrade equity(String tradeRef) {
        return EquityTrade.builder()
                .tradeRef(tradeRef)
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETR")
                .lotSize(100)
                .build();
    }
}
