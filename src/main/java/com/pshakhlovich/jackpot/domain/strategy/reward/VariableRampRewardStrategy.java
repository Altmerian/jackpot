package com.pshakhlovich.jackpot.domain.strategy.reward;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.service.dto.RewardResult;

/**
 * Variable ramp reward strategy implementation.
 * <p>
 * This strategy uses a probability that increases linearly as the jackpot pool grows,
 * creating increasing urgency and excitement as the pool approaches its cap. When the
 * pool is small, the win probability is low (base probability). As the pool grows toward
 * the reward cap, the probability ramps up linearly to a maximum probability (potentially
 * reaching 100% when the pool hits the cap).
 * </p>
 * <p>
 * <strong>Formula:</strong>
 * </p>
 * <pre>
 * poolRatio = min(currentPool / rewardCap, 1.0)
 * probability = min(baseProbability + (rampRate * poolRatio), maxProbability)
 * win = randomDraw &lt; probability
 * payout = min(currentPool, rewardCap) if win, else 0
 * </pre>
 * <p>
 * <strong>Example:</strong>
 * </p>
 * <ul>
 *   <li>baseProbability = 0.001 (0.1% base chance)</li>
 *   <li>maxProbability = 1.0 (100% maximum chance)</li>
 *   <li>rampRate = 0.999 (99.9% total increase range)</li>
 *   <li>rewardCap = 100,000.00</li>
 *   <li>currentPool = 75,000.00</li>
 * </ul>
 * <p>
 * Then: {@code poolRatio = 75,000 / 100,000 = 0.75}<br>
 * {@code probability = 0.001 + (0.999 * 0.75) = 0.75025 (75% chance!)}<br>
 * {@code randomDraw = 0.500 < 0.75025 = true (WIN!)}<br>
 * {@code payout = min(75,000.00, 100,000.00) = 75,000.00}
 * </p>
 * <p>
 * This creates a "must-hit-by" mechanic where the jackpot becomes increasingly likely to
 * pay out as it approaches the cap, guaranteeing a win when the pool reaches the cap.
 * </p>
 * <p>
 * <strong>Required jackpot configuration:</strong>
 * </p>
 * <ul>
 *   <li>{@code rewardBaseProbability} - The minimum probability when pool is empty (e.g., 0.001 for 0.1%)</li>
 *   <li>{@code rewardMaxProbability} - The maximum probability (ceiling, e.g., 1.0 for 100%)</li>
 *   <li>{@code rewardRampRate} - How much probability increases as pool fills (typically maxProb - baseProb)</li>
 *   <li>{@code rewardCap} - The pool size at which probability reaches maximum (must be positive)</li>
 * </ul>
 *
 * @see RewardStrategy
 * @see RewardStrategyType#VARIABLE_RAMP
 */
@Component
public class VariableRampRewardStrategy implements RewardStrategy {

    @Override
    public RewardStrategyType type() {
        return RewardStrategyType.VARIABLE_RAMP;
    }

    /**
     * Evaluates a bet for jackpot reward using variable ramping probability.
     *
     * @param jackpot the jackpot being evaluated (must have all required configuration)
     * @param randomDraw a random value in [0.0, 1.0) used to determine win
     * @return the reward evaluation result
     * @throws IllegalStateException if required configuration is missing or invalid
     */
    @Override
    public RewardResult evaluate(Jackpot jackpot, double randomDraw) {
        // Validate and retrieve all required configuration parameters
        BigDecimal baseProbability = require(jackpot.getRewardBaseProbability(), "Variable ramp strategy requires base probability");
        BigDecimal maxProbability = require(jackpot.getRewardMaxProbability(), "Variable ramp strategy requires max probability");
        BigDecimal rampRate = require(jackpot.getRewardRampRate(), "Variable ramp strategy requires ramp rate");
        BigDecimal rewardCap = require(jackpot.getRewardCap(), "Variable ramp strategy requires reward cap");

        if (rewardCap.signum() <= 0) {
            throw new IllegalStateException("rewardCap must be positive");
        }

        // Calculate the pool ratio: how full is the pool compared to the cap?
        // Range: [0.0, 1.0] where 0.0 = empty pool, 1.0 = pool at or above cap
        BigDecimal poolRatio = jackpot.getCurrentPool()
                .divide(rewardCap, 6, RoundingMode.HALF_UP); // Use 6 decimal precision for probability calculations

        // Cap the ratio at 1.0 if pool exceeds the reward cap
        if (poolRatio.compareTo(BigDecimal.ONE) > 0) {
            poolRatio = BigDecimal.ONE;
        }

        // Calculate probability using linear ramp formula:
        // probability = baseProbability + (rampRate * poolRatio)
        //
        // Example with poolRatio = 0.75:
        //   baseProbability = 0.001 (0.1%)
        //   rampRate = 0.999 (99.9% increase range)
        //   probability = 0.001 + (0.999 * 0.75) = 0.75025 (75%)
        //
        // This creates increasing urgency as the pool fills:
        //   Pool at 25% → ~25% win chance
        //   Pool at 50% → ~50% win chance
        //   Pool at 75% → ~75% win chance
        //   Pool at 100% → 100% win chance (guaranteed)
        BigDecimal probability = baseProbability.add(rampRate.multiply(poolRatio));

        // Enforce the maximum probability ceiling
        // This prevents probability from exceeding 100% or other configured limits
        if (probability.compareTo(maxProbability) > 0) {
            probability = maxProbability;
        }

        // Determine if this bet wins by comparing random draw to calculated probability
        // Example: randomDraw=0.500 < probability=0.75025 → WIN!
        boolean win = BigDecimal.valueOf(randomDraw).compareTo(probability) < 0;

        BigDecimal payout = BigDecimal.ZERO;

        if (win) {
            // Calculate payout as the lesser of current pool and reward cap
            // This ensures we never pay out more than configured maximum
            payout = jackpot.getCurrentPool().min(rewardCap).setScale(2, RoundingMode.HALF_UP);

            // Reset the jackpot pool to its initial value after winning
            // This starts a new jackpot cycle from the beginning
            jackpot.resetPoolToInitial();
        }

        // Return comprehensive result with strategy type, calculated probability,
        // payout amount, updated pool, and win status
        return new RewardResult(
                RewardStrategyType.VARIABLE_RAMP,
                probability.setScale(6, RoundingMode.HALF_UP),
                payout,
                jackpot.getCurrentPool(),
                win);
    }
}
