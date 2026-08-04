package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        try {
            var result = kafkaTemplate.send(topic, event.tradeRef(), event).get(10, TimeUnit.SECONDS);
            if (log.isDebugEnabled()) {
                log.debug("Published TradeEvent tradeRef={} -> partition={} offset={}",
                        event.tradeRef(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing TradeEvent tradeRef={} action={}",
                    event.tradeRef(), event.action(), e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to publish TradeEvent tradeRef={} action={}",
                    event.tradeRef(), event.action(), e);
        }
    }
}
