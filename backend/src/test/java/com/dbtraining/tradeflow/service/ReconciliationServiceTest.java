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
        service = new ReconciliationService(reconResultRepository, meterRegistry);
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
								/* TODO different price */, LocalDate.of(2026, 3, 1));

		ReconReport report = service.matchTrades(List.of(in), List.of(out));

    // TODO: 3 assertions
	}

private static BaseTrade equityWith(String ref, BigDecimal qty, BigDecimal price, LocalDate date) {
    /* TODO build EquityTrade */
    return null;
}
}


