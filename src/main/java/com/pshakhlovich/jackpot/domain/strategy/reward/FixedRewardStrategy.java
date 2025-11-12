package com.pshakhlovich.jackpot.domain.strategy.reward;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.service.dto.RewardResult;

/**
 * Fixed probability reward strategy implementation.
 * <p>
 * This strategy uses a constant probability for determining jackpot wins, regardless
 * of the current pool size. Each evaluation has the same chance of winning, providing
 * predictable and consistent odds for players.
 * </p>
 * <p>
 * <strong>Formula:</strong>
 * </p>
 * <pre>
 * win = randomDraw &lt; rewardBaseProbability
 * payout = min(currentPool, rewardCap) if win, else 0
 * </pre>
 * <p>
 * <strong>Example:</strong>
 * </p>
 * <ul>
 *   <li>rewardBaseProbability = 0.01 (1% chance to win)</li>
 *   <li>rewardCap = 50,000.00 (maximum payout)</li>
 *   <li>currentPool = 30,000.00</li>
 *   <li>randomDraw = 0.005 (from RNG)</li>
 * </ul>
 * <p>
 * Then: {@code 0.005 < 0.01 = true (WIN!)}<br>
 * {@code payout = min(30,000.00, 50,000.00) = 30,000.00}<br>
 * Pool is reset to initial value after payout.
 * </p>
 * <p>
 * <strong>Required jackpot configuration:</strong>
 * </p>
 * <ul>
 *   <li>{@code rewardBaseProbability} - The fixed probability of winning (e.g., 0.01 for 1%)</li>
 *   <li>{@code rewardCap} - The maximum payout amount (prevents excessive payouts)</li>
 * </ul>
 *
 * @see RewardStrategy
 * @see RewardStrategyType#FIXED
 */
@Component
public class FixedRewardStrategy implements RewardStrategy {

    @Override
    public RewardStrategyType type() {
        return RewardStrategyType.FIXED;
    }

    /**
     * Evaluates a bet for jackpot reward using fixed probability.
     *
     * @param jackpot the jackpot being evaluated (must have required configuration)
     * @param randomDraw a random value in [0.0, 1.0) used to determine win
     * @return the reward evaluation result
     * @throws IllegalStateException if required configuration is missing
     */
    @Override
    public RewardResult evaluate(Jackpot jackpot, double randomDraw) {
        // Retrieve fixed probability and maximum payout configuration
        BigDecimal probability = require(jackpot.getRewardBaseProbability(), "Fixed reward strategy requires rewardBaseProbability");
        BigDecimal rewardCap = require(jackpot.getRewardCap(), "Fixed reward strategy requires rewardCap");

        // Determine if this bet wins by comparing random draw to fixed probability
        // Example: randomDraw=0.005 < probability=0.01 → WIN!
        boolean win = BigDecimal.valueOf(randomDraw).compareTo(probability) < 0;

        BigDecimal payout = BigDecimal.ZERO;

        if (win) {
            // Calculate payout as the lesser of current pool and reward cap
            // This ensures we never pay out more than configured maximum
            payout = jackpot.getCurrentPool().min(rewardCap).setScale(2, RoundingMode.HALF_UP);

            // Reset the jackpot pool to its initial value after winning
            // This starts a new jackpot cycle
            jackpot.resetPoolToInitial();
        }

        // Return comprehensive result with strategy type, probability, payout, updated pool, and win status
        return new RewardResult(
                RewardStrategyType.FIXED,
                probability.setScale(6, RoundingMode.HALF_UP),
                payout,
                jackpot.getCurrentPool(),
                win);
    }
}
