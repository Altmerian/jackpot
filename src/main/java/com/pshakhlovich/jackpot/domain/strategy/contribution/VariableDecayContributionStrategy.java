package com.pshakhlovich.jackpot.domain.strategy.contribution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.service.dto.ContributionResult;

/**
 * Variable decay contribution strategy implementation.
 * <p>
 * This strategy applies a contribution rate that decreases linearly as the jackpot pool grows.
 * When the pool is empty, the contribution rate is at its maximum (base rate). As the pool
 * approaches a configured threshold, the rate decays linearly down to a minimum rate. This
 * creates an incentive for early betting while ensuring the pool continues to grow over time.
 * </p>
 * <p>
 * <strong>Formula:</strong>
 * </p>
 * <pre>
 * poolRatio = min(currentPool / decayThreshold, 1.0)
 * effectiveRate = max(baseRate - (decaySlope * poolRatio), minRate)
 * contribution = betAmount * effectiveRate
 * </pre>
 * <p>
 * <strong>Example:</strong>
 * </p>
 * <ul>
 *   <li>baseRate = 0.10 (10%)</li>
 *   <li>minRate = 0.02 (2%)</li>
 *   <li>decaySlope = 0.08 (8% total decay range)</li>
 *   <li>decayThreshold = 10,000.00</li>
 *   <li>currentPool = 5,000.00</li>
 * </ul>
 * <p>
 * Then: {@code poolRatio = 5,000 / 10,000 = 0.5}<br>
 * {@code effectiveRate = 0.10 - (0.08 * 0.5) = 0.06 (6%)}<br>
 * For a bet of $100: {@code contribution = 100 * 0.06 = $6.00}
 * </p>
 * <p>
 * <strong>Required jackpot configuration:</strong>
 * </p>
 * <ul>
 *   <li>{@code contributionRate} - The base/maximum contribution rate when pool is empty</li>
 *   <li>{@code minContributionRate} - The minimum contribution rate (floor)</li>
 *   <li>{@code decaySlope} - How much the rate decreases as pool fills (typically baseRate - minRate)</li>
 *   <li>{@code decayThreshold} - The pool size at which the rate reaches minimum (must be positive)</li>
 * </ul>
 *
 * @see ContributionStrategy
 * @see ContributionStrategyType#VARIABLE_DECAY
 */
@Component
public class VariableDecayContributionStrategy implements ContributionStrategy {

    @Override
    public ContributionStrategyType type() {
        return ContributionStrategyType.VARIABLE_DECAY;
    }

    /**
     * Calculates a variable contribution that decays as the pool grows.
     *
     * @param jackpot the jackpot to contribute to (must have all required configuration)
     * @param betAmount the bet amount to calculate contribution from
     * @return the contribution result with calculated amount and updated pool
     * @throws IllegalStateException if required configuration is missing or invalid
     */
    @Override
    public ContributionResult contribute(Jackpot jackpot, BigDecimal betAmount) {
        // Validate and retrieve all required configuration parameters
        BigDecimal baseRate = require(jackpot.getContributionRate(), "Variable decay strategy requires contributionRate configuration");
        BigDecimal minRate = require(jackpot.getMinContributionRate(), "Variable decay strategy requires minContributionRate configuration");
        BigDecimal decaySlope = require(jackpot.getDecaySlope(), "Variable decay strategy requires decaySlope configuration");
        BigDecimal decayThreshold = require(jackpot.getDecayThreshold(), "Variable decay strategy requires decayThreshold configuration");

        if (decayThreshold.signum() <= 0) {
            throw new IllegalStateException("decayThreshold must be positive");
        }

        // Calculate the pool ratio: how full is the pool compared to the threshold?
        // Range: [0.0, 1.0] where 0.0 = empty pool, 1.0 = pool at or above threshold
        BigDecimal poolRatio = jackpot.getCurrentPool()
                .divide(decayThreshold, 8, RoundingMode.HALF_UP); // Use high precision for intermediate calculation

        // Cap the ratio at 1.0 if pool exceeds threshold
        if (poolRatio.compareTo(BigDecimal.ONE) > 0) {
            poolRatio = BigDecimal.ONE;
        }

        // Calculate effective rate using linear decay formula:
        // effectiveRate = baseRate - (decaySlope * poolRatio)
        //
        // Example with poolRatio = 0.5:
        //   baseRate = 0.10 (10%)
        //   decaySlope = 0.08 (8% decay range)
        //   effectiveRate = 0.10 - (0.08 * 0.5) = 0.06 (6%)
        BigDecimal effectiveRate = baseRate.subtract(decaySlope.multiply(poolRatio));

        // Enforce the minimum rate floor to ensure pool always grows
        if (effectiveRate.compareTo(minRate) < 0) {
            effectiveRate = minRate;
        }

        // Calculate the actual contribution using the effective rate
        BigDecimal contribution = betAmount.multiply(effectiveRate)
                .setScale(2, RoundingMode.HALF_UP); // Round to 2 decimal places for currency

        // Update the jackpot pool by adding the contribution
        BigDecimal updatedPool = jackpot.increasePool(contribution);

        return new ContributionResult(
                ContributionStrategyType.VARIABLE_DECAY,
                contribution,
                updatedPool);
    }
}
