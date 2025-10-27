package com.pshakhlovich.jackpot.api.dto;

import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import java.math.BigDecimal;

public record JackpotEvaluationResponse(
        boolean win,
        BigDecimal payoutAmount,
        BigDecimal currentJackpotPool,
        BigDecimal probability,
        RewardStrategyType strategy,
        String betId,
        String jackpotId
) {
}
