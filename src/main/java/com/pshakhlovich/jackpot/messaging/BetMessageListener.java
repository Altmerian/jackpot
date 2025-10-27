package com.pshakhlovich.jackpot.messaging;

import com.pshakhlovich.jackpot.avro.Bet;
import com.pshakhlovich.jackpot.config.KafkaTopicsConfig;
import com.pshakhlovich.jackpot.service.ContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BetMessageListener {

    private final ContributionService contributionService;

    @Transactional("transactionManager")
    @KafkaListener(topics = KafkaTopicsConfig.BETS_TOPIC, containerFactory = "betListenerContainerFactory")
    public void onBet(@Payload Bet bet) {
        log.debug("Received bet {} for jackpot {}", bet.getBetId(), bet.getJackpotId());
        contributionService.applyContribution(bet);
    }
}
