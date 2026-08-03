package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventProducer.class);

    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    private final String topic;

    public TradeEventProducer(KafkaTemplate<String, TradeEvent> kafkaTemplate,
                              @Value("${tradeflow.kafka.topics.trades:trade-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(TradeEvent event) {
        if (kafkaTemplate == null || topic == null) {
            log.warn("KafkaTemplate or topic not initialized, skipping publish for tradeRef={}", event.tradeRef());
            return;
        }
        kafkaTemplate.send(topic, event.tradeRef(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TradeEvent tradeRef={} action={}",
                                event.tradeRef(), event.action(), ex);
                    } else if (log.isDebugEnabled()) {
                        log.debug("Published TradeEvent tradeRef={} -> partition={} offset={}",
                                event.tradeRef(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
