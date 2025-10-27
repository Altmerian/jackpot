package com.pshakhlovich.jackpot.domain.strategy.reward;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.service.dto.RewardResult;

@Component
public class FixedRewardStrategy implements RewardStrategy {

    @Override
    public RewardStrategyType type() {
        return RewardStrategyType.FIXED;
    }

    @Override
    public RewardResult evaluate(Jackpot jackpot, double randomDraw) {
        BigDecimal probability = require(jackpot.getRewardBaseProbability(), "Fixed reward strategy requires rewardBaseProbability");
        BigDecimal rewardCap = require(jackpot.getRewardCap(), "Fixed reward strategy requires rewardCap");

        boolean win = BigDecimal.valueOf(randomDraw).compareTo(probability) < 0;
        BigDecimal payout = BigDecimal.ZERO;

        if (win) {
            payout = jackpot.getCurrentPool().min(rewardCap).setScale(2, RoundingMode.HALF_UP);
            jackpot.resetPoolToInitial();
        }

        return new RewardResult(
                RewardStrategyType.FIXED,
                probability.setScale(6, RoundingMode.HALF_UP),
                payout,
                jackpot.getCurrentPool(),
                win);
    }

    private BigDecimal require(BigDecimal value, String message) {
        return Objects.requireNonNull(value, message);
    }
}
