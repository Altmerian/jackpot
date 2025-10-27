package com.pshakhlovich.jackpot.config;

import com.pshakhlovich.jackpot.avro.Bet;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    private static final String TRANSACTION_ID_PREFIX = "jackpot-producer";

    @Bean
    public ProducerFactory<String, Bet> betProducerFactory(KafkaProperties kafkaProperties) {
        var props = kafkaProperties.buildProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class);
        DefaultKafkaProducerFactory<String, Bet> factory = new DefaultKafkaProducerFactory<>(props);
        factory.setTransactionIdPrefix(TRANSACTION_ID_PREFIX);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Bet> betKafkaTemplate(ProducerFactory<String, Bet> betProducerFactory) {
        KafkaTemplate<String, Bet> template = new KafkaTemplate<>(betProducerFactory);
        template.setObservationEnabled(true);
        return template;
    }
}
