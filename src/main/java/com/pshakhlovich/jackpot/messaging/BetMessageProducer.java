package com.pshakhlovich.jackpot.messaging;

import com.pshakhlovich.jackpot.avro.Bet;
import com.pshakhlovich.jackpot.config.KafkaTopicsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BetMessageProducer {

    private final KafkaTemplate<String, Bet> kafkaTemplate;

    public void publish(Bet bet) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send(KafkaTopicsConfig.BETS_TOPIC, bet.getJackpotId(), bet)
                    .whenComplete((result, throwable) -> handleResult(bet, result, throwable));
            return null;
        });
    }

    private void handleResult(Bet bet, SendResult<String, Bet> result, Throwable throwable) {
        if (throwable != null) {
            log.error("Failed to publish bet {} to topic {}", bet.getBetId(), KafkaTopicsConfig.BETS_TOPIC, throwable);
            return;
        }

        if (result == null || result.getRecordMetadata() == null) {
            log.warn("Published bet {} but metadata unavailable", bet.getBetId());
            return;
        }

        log.info("Published bet {} to {}-{} offset {}", bet.getBetId(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }
}
