package com.pshakhlovich.jackpot.domain.strategy.contribution;

import java.math.BigDecimal;
import java.util.Objects;

import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.service.dto.ContributionResult;

/**
 * Strategy interface for calculating jackpot contributions from bet amounts.
 * <p>
 * Implementations of this interface define different algorithms for determining
 * how much of a bet should be contributed to a jackpot pool.
 * </p>
 * <p>
 * All implementations must be registered as Spring components to be automatically
 * discovered and registered by {@link com.pshakhlovich.jackpot.domain.strategy.StrategyRegistry}.
 * </p>
 *
 * @see com.pshakhlovich.jackpot.domain.strategy.StrategyRegistry
 */
public interface ContributionStrategy {

    /**
     * Returns the strategy type identifier for this implementation.
     * <p>
     * This type is used by the strategy registry for lookup and must be unique
     * across all contribution strategy implementations.
     * </p>
     *
     * @return the strategy type
     */
    ContributionStrategyType type();

    /**
     * Calculates the contribution amount for a bet and updates the jackpot pool.
     * <p>
     * This method encapsulates the core contribution logic, calculating how much
     * of the bet amount should be added to the jackpot pool based on the strategy's
     * algorithm and the jackpot's current state and configuration.
     * </p>
     * <p>
     * <strong>Side effect:</strong> This method modifies the jackpot's current pool
     * by calling {@link Jackpot#increasePool(BigDecimal)}.
     * </p>
     *
     * @param jackpot the jackpot to which the contribution will be applied (must not be null)
     * @param betAmount the bet amount from which to calculate the contribution (must not be null, must be positive)
     * @return a result object containing the calculated contribution amount and updated pool size
     * @throws IllegalStateException if the jackpot is missing required configuration for this strategy
     * @throws NullPointerException if jackpot or betAmount is null
     */
    ContributionResult contribute(Jackpot jackpot, BigDecimal betAmount);

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
