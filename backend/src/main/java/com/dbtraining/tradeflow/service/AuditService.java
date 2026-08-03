package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.model.AuditLog;
import com.dbtraining.tradeflow.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(TradeEvent event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event.payload());
            AuditLog logEntry = AuditLog.builder()
                    .tableName("trades")
                    .operation(AuditLog.Operation.I)
                    .rowPk(event.payload().id())
                    .afterData(payloadJson)
                    .changedBy("kafka-consumer")
                    .build();
            auditLogRepository.save(logEntry);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize TradeEvent payload for audit", ex);
        }
    }
}
