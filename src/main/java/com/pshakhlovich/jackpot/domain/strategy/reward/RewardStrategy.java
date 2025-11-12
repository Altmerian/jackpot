package com.pshakhlovich.jackpot.domain.strategy.reward;

import java.math.BigDecimal;
import java.util.Objects;

import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.service.dto.RewardResult;

/**
 * Strategy interface for evaluating jackpot reward eligibility and calculating payouts.
 * <p>
 * Implementations of this interface define different algorithms for determining
 * whether a bet wins the jackpot and how much should be paid out.
 * </p>
 * <p>
 * All implementations must be registered as Spring components to be automatically
 * discovered and registered by {@link com.pshakhlovich.jackpot.domain.strategy.StrategyRegistry}.
 * </p>
 *
 * @see com.pshakhlovich.jackpot.domain.strategy.StrategyRegistry
 */
public interface RewardStrategy {

    /**
     * Returns the strategy type identifier for this implementation.
     * <p>
     * This type is used by the strategy registry for lookup and must be unique
     * across all reward strategy implementations.
     * </p>
     *
     * @return the strategy type
     */
    RewardStrategyType type();

    /**
     * Evaluates whether a bet wins the jackpot and calculates the payout.
     * <p>
     * This method implements the core reward evaluation logic, using the provided
     * random draw value to determine if the bet wins based on the strategy's
     * probability calculation. The probability itself may vary based on the
     * jackpot's current state and configuration.
     * </p>
     * <p>
     * <strong>Side effect:</strong> If the bet wins, this method resets the jackpot
     * pool to its initial value by calling {@link Jackpot#resetPoolToInitial()}.
     * </p>
     *
     * @param jackpot the jackpot being evaluated (must not be null)
     * @param randomDraw a random value in the range [0.0, 1.0) used to determine if the bet wins
     * @return a result object containing the evaluation outcome, probability, and payout details
     * @throws IllegalStateException if the jackpot is missing required configuration for this strategy
     * @throws NullPointerException if jackpot is null
     */
    RewardResult evaluate(Jackpot jackpot, double randomDraw);

    /**
     * Helper method to validate required configuration values.
     *
     * @param value the configuration value to check
     * @param message the error message if value is null
     * @return the value if not null
     * @throws NullPointerException if value is null
     */
    default BigDecimal require(BigDecimal value, String message) {
        return Objects.requireNonNull(value, message);
    }
}
