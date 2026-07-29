package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * ReconciliationServiceTest — Day 3 Unit Tests
 * ============================================================================
 */
class ReconciliationServiceTest {

    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService();
    }

    @Test
    @DisplayName("Day 3: matchTrades with matching trades returns 0 discrepancies")
    void matchTrades_allMatched_returnsEmptyDiscrepancies() {
        BaseTrade trade1 = EquityTrade.builder()
                .tradeRef("TRD-1001")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("12.50"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA")
                .lotSize(100)
                .build();

        ReconReport report = service.matchTrades(List.of(trade1), List.of(trade1));

        assertEquals(1, report.matched().size());
        assertTrue(report.discrepancies().isEmpty());
    }

    @Test
    @DisplayName("Day 3: matchTrades with price difference flags PRICE_MISMATCH")
    void matchTrades_priceMismatch_flagsDiscrepancy() {
        BaseTrade internalTrade = EquityTrade.builder()
                .tradeRef("TRD-1002")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("12.50"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA")
                .lotSize(100)
                .build();

        BaseTrade externalTrade = EquityTrade.builder()
                .tradeRef("TRD-1002")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("12.55"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA")
                .lotSize(100)
                .build();

        ReconReport report = service.matchTrades(List.of(internalTrade), List.of(externalTrade));

        assertEquals(0, report.matched().size());
        assertEquals(1, report.discrepancies().size());
        assertEquals(1, report.discrepancies().get(0).types().size());
        assertEquals(DiscrepancyType.PRICE_MISMATCH, report.discrepancies().get(0).types().get(0));
    }

    @Test
    @DisplayName("Day 3: matchTrades with missing external trade flags MISSING_TRADE")
    void matchTrades_missingExternal_flagsMissingTrade() {
        BaseTrade internalTrade = EquityTrade.builder()
                .tradeRef("TRD-1003")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("50"))
                .price(new BigDecimal("5.00"))
                .tradeDate(LocalDate.of(2026, 1, 2))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA")
                .lotSize(100)
                .build();

        ReconReport report = service.matchTrades(List.of(internalTrade), List.of());

        assertEquals(0, report.matched().size());
        assertEquals(1, report.discrepancies().size());
        assertEquals(DiscrepancyType.MISSING_TRADE, report.discrepancies().get(0).types().get(0));
    }

    @Test
    @DisplayName("Day 3: generateReport correctly sums breakdown counts")
    void generateReport_computesSummaryBreakdown() {
        BaseTrade trade1 = EquityTrade.builder()
                .tradeRef("TRD-1004")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("10.00"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA")
                .lotSize(100)
                .build();

        ReconReport report = service.matchTrades(List.of(trade1), List.of());
        ReconSummary summary = service.generateReport(report);

        assertNotNull(summary);
        assertEquals(1, summary.totalInternal());
        assertEquals(0, summary.totalExternal());
        assertEquals(0, summary.matchedCount());
        assertEquals(1, summary.unmatchedCount());
        assertEquals(1, summary.breakdownByType().get(DiscrepancyType.MISSING_TRADE));
    }

    @Test
    @DisplayName("Day 3: render formats report text")
    void render_formatsSummaryOutput() {
        BaseTrade trade1 = EquityTrade.builder()
                .tradeRef("TRD-1005")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("10.00"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA")
                .lotSize(100)
                .build();

        ReconReport report = service.matchTrades(List.of(trade1), List.of(trade1));
        ReconSummary summary = service.generateReport(report);
        String text = service.render(summary);

        assertNotNull(text);
        assertTrue(text.contains("Reconciliation summary"));
        assertTrue(text.contains("Internal trades : 1"));
    }
}
