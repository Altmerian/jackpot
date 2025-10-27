package com.pshakhlovich.jackpot.domain.strategy.reward;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.service.dto.RewardResult;

@Component
public class VariableRampRewardStrategy implements RewardStrategy {

    @Override
    public RewardStrategyType type() {
        return RewardStrategyType.VARIABLE_RAMP;
    }

    @Override
    public RewardResult evaluate(Jackpot jackpot, double randomDraw) {
        BigDecimal baseProbability = require(jackpot.getRewardBaseProbability(), "Variable ramp strategy requires base probability");
        BigDecimal maxProbability = require(jackpot.getRewardMaxProbability(), "Variable ramp strategy requires max probability");
        BigDecimal rampRate = require(jackpot.getRewardRampRate(), "Variable ramp strategy requires ramp rate");
        BigDecimal rewardCap = require(jackpot.getRewardCap(), "Variable ramp strategy requires reward cap");

        if (rewardCap.signum() <= 0) {
            throw new IllegalStateException("rewardCap must be positive");
        }

        BigDecimal poolRatio = jackpot.getCurrentPool()
                .divide(rewardCap, 6, RoundingMode.HALF_UP);
        if (poolRatio.compareTo(BigDecimal.ONE) > 0) {
            poolRatio = BigDecimal.ONE;
        }

        BigDecimal probability = baseProbability.add(rampRate.multiply(poolRatio));
        if (probability.compareTo(maxProbability) > 0) {
            probability = maxProbability;
        }

        boolean win = BigDecimal.valueOf(randomDraw).compareTo(probability) < 0;
        BigDecimal payout = BigDecimal.ZERO;

        if (win) {
            payout = jackpot.getCurrentPool().min(rewardCap).setScale(2, RoundingMode.HALF_UP);
            jackpot.resetPoolToInitial();
        }

        return new RewardResult(
                RewardStrategyType.VARIABLE_RAMP,
                probability.setScale(6, RoundingMode.HALF_UP),
                payout,
                jackpot.getCurrentPool(),
                win);
    }

    private BigDecimal require(BigDecimal value, String message) {
        return Objects.requireNonNull(value, message);
    }
}
