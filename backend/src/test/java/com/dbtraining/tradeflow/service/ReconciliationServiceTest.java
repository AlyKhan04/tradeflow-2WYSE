package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.model.*;
import com.dbtraining.tradeflow.repository.ReconResultRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock private ReconResultRepository reconResultRepository;
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(); 
    }

    @Test
    void matchTrades_allMatched_returnsEmptyDiscrepancies() {
        List<BaseTrade> internal = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));
        List<BaseTrade> external = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.matched()).hasSize(3);
        assertThat(report.totalInternal()).isEqualTo(3);
        assertThat(report.totalExternal()).isEqualTo(3);
    }

    private static BaseTrade equity(String tradeRef) {
        return EquityTrade.builder()
                .tradeRef(tradeRef).instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA").lotSize(100)
                .build();
    }
	
	@Test
        void matchTrades_priceMismatch_flagsDiscrepancy() {
        BaseTrade in  = equityWith("TRD-001", new BigDecimal("100"),
                               new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));
        BaseTrade out = equityWith("TRD-001", new BigDecimal("100"),
                               new BigDecimal("249.99"), LocalDate.of(2026, 3, 1));

        ReconReport report = service.matchTrades(List.of(in), List.of(out));

        assertThat(report.matched()).isEmpty();
        assertThat(report.discrepancies()).hasSize(1);
        var d = report.discrepancies().get(0); 
        assertThat(d.tradeRef()).isEqualTo("TRD-001");
        assertThat(d.types()).containsExactly(DiscrepancyType.PRICE_MISMATCH);
    }

/** Scale-difference regression test: 245.5 vs 245.50 are equal by compareTo. */
    @Test
        void matchTrades_priceScaleDifference_notFlagged() {
        BaseTrade in  = equityWith("TRD-002", new BigDecimal("100"),
                               new BigDecimal("245.5"),  LocalDate.of(2026, 3, 1));
        BaseTrade out = equityWith("TRD-002", new BigDecimal("100"),
                               new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));

        ReconReport report = service.matchTrades(List.of(in), List.of(out));

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.matched()).hasSize(1);
    }

    private static BaseTrade equityWith(String tradeRef, BigDecimal qty, BigDecimal price, LocalDate date) {
    return EquityTrade.builder()
            .tradeRef(tradeRef).instrumentId(1L).counterpartyId(1L)
            .quantity(qty).price(price).tradeDate(date)
            .status(TradeStatus.MATCHED).exchange("XETRA").lotSize(100)
            .build();
}
}