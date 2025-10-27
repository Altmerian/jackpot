package com.pshakhlovich.jackpot.service;

import com.pshakhlovich.jackpot.api.dto.BetRequest;
import com.pshakhlovich.jackpot.api.mapper.BetMapper;
import com.pshakhlovich.jackpot.avro.Bet;
import com.pshakhlovich.jackpot.messaging.BetMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BetPublishingService {

    private final BetMapper betMapper;
    private final BetMessageProducer betMessageProducer;
    private final JackpotService jackpotService;

    public String publish(BetRequest request) {
        jackpotService.getRequired(request.jackpotId());
        Bet bet = betMapper.toAvro(request);
        betMessageProducer.publish(bet);
        log.debug("Bet {} published for jackpot {}", bet.getBetId(), bet.getJackpotId());
        return bet.getBetId().toString();
    }
}
