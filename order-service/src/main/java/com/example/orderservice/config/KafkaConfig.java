package com.example.orderservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:order-group}")
    private String groupId;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonDeserializer.class);
        props.put("spring.json.trusted.packages", "com.example.saga.event");
        props.put("spring.json.use.type.headers", true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // DLQ: Kirim message gagal ke topic {original-topic}.DLT setelah semua retry habis
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (record, ex) -> {
                String dlqTopic = record.topic() + ".DLT";
                Throwable rootCause = ex.getCause() != null ? ex.getCause() : ex;
                log.error("[ORDER-DLQ]     ✗ ═══════════════════════════════════════════");
                log.error("[ORDER-DLQ]     ✗ PESAN GAGAL → Masuk ke Karantina (DLT)");
                log.error("[ORDER-DLQ]       Topic Asal  : {}", record.topic());
                log.error("[ORDER-DLQ]       Topic DLT   : {}", dlqTopic);
                log.error("[ORDER-DLQ]       Offset      : {} | Partition: {}", record.offset(), record.partition());
                log.error("[ORDER-DLQ]       Jenis Error : {}", rootCause.getClass().getSimpleName());
                log.error("[ORDER-DLQ]       Penyebab    : {}", rootCause.getMessage());
                log.error("[ORDER-DLQ]     ✗ ═══════════════════════════════════════════");
                return new org.apache.kafka.common.TopicPartition(dlqTopic, record.partition());
            });

        // Retry 3x dengan jeda 2 detik, lalu kirim ke DLQ
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
