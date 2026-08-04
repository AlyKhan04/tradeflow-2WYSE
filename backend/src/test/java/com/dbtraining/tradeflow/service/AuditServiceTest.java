package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.model.AuditLog;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    @Test
    void record_persistsAuditLogFromTradeEvent() throws Exception {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditService service = new AuditService(repository, new ObjectMapper().findAndRegisterModules());

        TradeDto payload = new TradeDto(
                42L,
                "TRD-001",
                1L,
                2L,
                BigDecimal.ONE,
                new BigDecimal("10.50"),
                LocalDate.of(2024, 1, 15),
                TradeStatus.PENDING,
                Instant.now()
        );
        TradeEvent event = new TradeEvent("TRD-001", TradeEvent.Action.CREATED, Instant.now(), payload);

        service.record(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getTableName()).isEqualTo("trades");
        assertThat(saved.getOperation()).isEqualTo(AuditLog.Operation.I);
        assertThat(saved.getRowPk()).isEqualTo(42L);
        assertThat(saved.getAfterData()).contains("TRD-001");
    }
}
