package com.pshakhlovich.jackpot.domain.strategy.reward;

import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.service.dto.RewardResult;

public interface RewardStrategy {

    RewardStrategyType type();

    RewardResult evaluate(Jackpot jackpot, double randomDraw);
}
