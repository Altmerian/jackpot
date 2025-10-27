package com.pshakhlovich.jackpot.api.mapper;

import com.pshakhlovich.jackpot.api.dto.BetRequest;
import com.pshakhlovich.jackpot.avro.Bet;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BetMapper {

    private final Clock clock;

    public Bet toAvro(BetRequest request) {
        Instant now = Instant.now(clock);
        return Bet.newBuilder()
                .setBetId(request.betId())
                .setUserId(request.userId())
                .setJackpotId(request.jackpotId())
                .setBetAmount(request.betAmount().doubleValue())
                .setCreatedAt(now)
                .build();
    }
}
