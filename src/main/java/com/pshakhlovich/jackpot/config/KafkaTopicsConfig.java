package com.pshakhlovich.jackpot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    public static final String BETS_TOPIC = "jackpot-bets";

    @Bean
    public NewTopic jackpotBetsTopic() {
        return TopicBuilder.name(BETS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
