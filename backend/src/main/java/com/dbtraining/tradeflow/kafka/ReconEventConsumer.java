package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReconEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReconEventConsumer.class);

    private final ReconciliationService reconService;

    public ReconEventConsumer(ReconciliationService reconService) {
        this.reconService = reconService;
    }

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades:trade-events}",
            groupId = "recon-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(TradeEvent event) {
        if (event.action() != TradeEvent.Action.CREATED) {
            return;  // only newly-created trades get auto-reconciled
        }
        log.info("Reconciling tradeRef={}", event.tradeRef());
        reconService.runForTrade(event.tradeRef());
    }
}
