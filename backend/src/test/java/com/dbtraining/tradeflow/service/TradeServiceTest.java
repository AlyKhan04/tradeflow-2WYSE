package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.CounterpartyRepository;
import com.dbtraining.tradeflow.repository.InstrumentRepository;
import com.dbtraining.tradeflow.repository.TradeRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeServiceTest {

    @Test
    void updateStatus_illegalTransitionFromPendingToSettled_throwsIllegalStateException() {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
        CounterpartyRepository counterpartyRepository = mock(CounterpartyRepository.class);

        Trade trade = Trade.builder()
                .id(1L)
                .tradeRef("TRD-2026-0001")
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("250.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.PENDING)
                .build();

        when(tradeRepository.findById(1L)).thenReturn(Optional.of(trade));

        TradeService service = new TradeService(
                tradeRepository,
                instrumentRepository,
                counterpartyRepository,
                new SimpleMeterRegistry()
        );

        assertThatThrownBy(() -> service.updateStatus(1L, TradeStatus.SETTLED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Illegal transition");
    }
}
