package com.dbtraining.tradeflow.config;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * KafkaConfig — TICKET-I118 (Day 9)
 * ============================================================================
 * WHAT:    Custom Kafka beans — DLT recoverer, error handler, consumer factory
 *          overrides.
 * HOW:     @Configuration class. Spring Boot auto-config covers most of it;
 *          this is where you override.
 * WHY:     Robust event pipelines need: retries, DLT for poison messages,
 *          observable error handling.
 * OBSERVE: A malformed message no longer crashes the consumer — it lands in
 *          `trade-events.DLT`.
 * ============================================================================
 *
 *  TODO(TICKET-I118):
 *    @Bean
 *    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String,Object> tpl) {
 *        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(tpl,
 *            (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
 *        return new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
 *    }
 *
 *  HINT: For at-least-once semantics, ack-mode should stay MANUAL_IMMEDIATE
 *        only if your consumer acks manually after successful processing.
 * ============================================================================
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final String dltTopic;

    public KafkaConfig(KafkaProperties kafkaProperties,
                       @Value("${tradeflow.kafka.topics.dlt:trade-events.DLT}") String dltTopic) {
        this.kafkaProperties = kafkaProperties;
        this.dltTopic = dltTopic;
    }

    @Bean
    public ConsumerFactory<String, TradeEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dbtraining.tradeflow.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeEvent>
            kafkaListenerContainerFactory(ConsumerFactory<String, TradeEvent> consumerFactory,
                                          DefaultErrorHandler errorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TradeEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(1000L, 3));

        // unrecoverable errors skip the retry loop and go straight to DLT
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(dltTopic).partitions(1).replicas(1).build();
    }
}