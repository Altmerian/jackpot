package com.pshakhlovich.jackpot.domain.strategy.contribution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.service.dto.ContributionResult;

@Component
public class VariableDecayContributionStrategy implements ContributionStrategy {

    @Override
    public ContributionStrategyType type() {
        return ContributionStrategyType.VARIABLE_DECAY;
    }

    @Override
    public ContributionResult contribute(Jackpot jackpot, BigDecimal betAmount) {
        BigDecimal baseRate = require(jackpot.getContributionRate(), "Variable decay strategy requires contributionRate configuration");
        BigDecimal minRate = require(jackpot.getMinContributionRate(), "Variable decay strategy requires minContributionRate configuration");
        BigDecimal decaySlope = require(jackpot.getDecaySlope(), "Variable decay strategy requires decaySlope configuration");
        BigDecimal decayThreshold = require(jackpot.getDecayThreshold(), "Variable decay strategy requires decayThreshold configuration");

        if (decayThreshold.signum() <= 0) {
            throw new IllegalStateException("decayThreshold must be positive");
        }

        BigDecimal poolRatio = jackpot.getCurrentPool()
                .divide(decayThreshold, 8, RoundingMode.HALF_UP);
        if (poolRatio.compareTo(BigDecimal.ONE) > 0) {
            poolRatio = BigDecimal.ONE;
        }

        BigDecimal effectiveRate = baseRate.subtract(decaySlope.multiply(poolRatio));
        if (effectiveRate.compareTo(minRate) < 0) {
            effectiveRate = minRate;
        }

        BigDecimal contribution = betAmount.multiply(effectiveRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal updatedPool = jackpot.increasePool(contribution);

        return new ContributionResult(
                ContributionStrategyType.VARIABLE_DECAY,
                contribution,
                updatedPool);
    }

    private BigDecimal require(BigDecimal value, String message) {
        return Objects.requireNonNull(value, message);
    }
}
