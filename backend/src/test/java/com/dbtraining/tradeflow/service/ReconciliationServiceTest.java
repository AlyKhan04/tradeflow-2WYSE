package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.dbtraining.tradeflow.model.ReconResult;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================================
 * ReconciliationServiceTest — TICKET-I048..I053
 * ============================================================================
 * WHAT:    JUnit + Mockito tests for the recon engine.
 * HOW:     @ExtendWith(MockitoExtension.class). Mock the DAOs, build sample
 *          trade lists, assert on the returned ReconReport.
 * WHY:     Day 4 sets a 70% coverage target. ReconciliationService is the
 *          critical path — it gets the most attention.
 * OBSERVE: `mvn test` runs these in a few seconds; JaCoCo report shows the
 *          coverage % per class.
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private TradeDAO tradeDAO;

    @Mock
    private ReconResultDAO reconResultDAO;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void matchTrades_allMatched_returnsEmptyDiscrepancies() {
        Trade internalTrade = Trade.builder()
                .tradeRef("TRD-1001")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("12.50"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .build();

        ReconciliationService service = new ReconciliationService(null, null);
        var report = service.matchTrades(List.of(internalTrade), List.of(internalTrade));

        assertEquals(1, report.matched().size());
        assertTrue(report.discrepancies().isEmpty());
    }

    @Test
    void matchTrades_priceMismatch_flagsDiscrepancy() {
        Trade internalTrade = Trade.builder()
                .tradeRef("TRD-1002")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("12.50"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .build();
        Trade externalTrade = Trade.builder()
                .tradeRef("TRD-1002")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("12.55"))
                .tradeDate(LocalDate.of(2026, 1, 1))
                .status(TradeStatus.MATCHED)
                .build();

        ReconciliationService service = new ReconciliationService(null, null);
        var report = service.matchTrades(List.of(internalTrade), List.of(externalTrade));

        assertEquals(0, report.matched().size());
        assertEquals(1, report.discrepancies().size());
        assertEquals(1, report.discrepancies().get(0).types().size());
        assertEquals(DiscrepancyType.PRICE_MISMATCH, report.discrepancies().get(0).types().get(0));
    }

    @Test
    void matchTrades_missingExternal_flagsMissingTrade() {
        Trade internalTrade = Trade.builder()
                .tradeRef("TRD-1003")
                .instrumentId(1L)
                .counterpartyId(10L)
                .quantity(new BigDecimal("50"))
                .price(new BigDecimal("5.00"))
                .tradeDate(LocalDate.of(2026, 1, 2))
                .status(TradeStatus.MATCHED)
                .build();

        ReconciliationService service = new ReconciliationService(null, null);
        var report = service.matchTrades(List.of(internalTrade), List.of());

        assertEquals(0, report.matched().size());
        assertEquals(1, report.discrepancies().size());
        assertEquals(DiscrepancyType.MISSING_TRADE, report.discrepancies().get(0).types().get(0));
    }

    @Test
    void mockedTradeDAO_findAllCalledOnce() {
        Trade internalTrade = Trade.builder()
                .tradeRef("TRD-1004")
                .instrumentId(2L)
                .counterpartyId(20L)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("1.00"))
                .tradeDate(LocalDate.of(2026, 1, 3))
                .status(TradeStatus.MATCHED)
                .build();
        when(tradeDAO.findAll()).thenReturn(List.of(internalTrade));

        reconciliationService.reconcileWithDatabase(List.of(internalTrade));

        verify(tradeDAO, times(1)).findAll();
    }

    @Test
    void mockedReconResultDAO_insertCalledPerDiscrepancy() {
        Trade internalTrade = Trade.builder()
                .tradeRef("TRD-1005")
                .instrumentId(2L)
                .counterpartyId(20L)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("1.00"))
                .tradeDate(LocalDate.of(2026, 1, 3))
                .status(TradeStatus.MATCHED)
                .build();
        Trade externalTrade = Trade.builder()
                .tradeRef("TRD-1005")
                .instrumentId(2L)
                .counterpartyId(20L)
                .quantity(new BigDecimal("11"))
                .price(new BigDecimal("1.00"))
                .tradeDate(LocalDate.of(2026, 1, 3))
                .status(TradeStatus.MATCHED)
                .build();

        when(tradeDAO.findAll()).thenReturn(List.of(internalTrade));
        when(reconResultDAO.insert(any())).thenReturn(1L);

        reconciliationService.reconcileWithDatabase(List.of(externalTrade));

        ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);

        verify(reconResultDAO, times(1)).insert(captor.capture());
        assertNotNull(captor.getValue());
    }
}
