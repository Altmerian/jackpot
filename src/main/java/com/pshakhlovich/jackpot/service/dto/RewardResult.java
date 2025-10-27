package com.pshakhlovich.jackpot.service.dto;

import java.math.BigDecimal;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;

public record RewardResult(
        RewardStrategyType strategy,
        BigDecimal probability,
        BigDecimal payoutAmount,
        BigDecimal updatedPool,
        boolean win
) {
}
