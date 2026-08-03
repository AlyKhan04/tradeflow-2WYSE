package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TradeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventConsumer.class);

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades:trade-events}",
            groupId = "trade-log-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(TradeEvent event) {
        log.info("Received TradeEvent[tradeRef={}, action={}]",
                event.tradeRef(), event.action());
    }
}
