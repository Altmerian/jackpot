package com.pshakhlovich.jackpot.domain.model;

/**
 * Defines the available strategies for calculating jackpot contributions from bet amounts.
 * <p>
 * Each strategy determines how much of a bet should be contributed to the jackpot pool,
 * allowing for different contribution models based on business requirements.
 * </p>
 */
public enum ContributionStrategyType {

    /**
     * Fixed contribution rate strategy.
     * <p>
     * Applies a constant percentage of the bet amount as contribution,
     * regardless of the current jackpot pool size.
     * </p>
     *
     * @see com.pshakhlovich.jackpot.domain.strategy.contribution.FixedContributionStrategy
     */
    FIXED_RATE,

    /**
     * Variable decay contribution strategy.
     * <p>
     * Applies a variable percentage that decreases as the jackpot pool grows.
     * Starts with a higher contribution rate when the pool is small and gradually
     * reduces the rate as the pool approaches a configured threshold, down to a minimum rate.
     * </p>
     *
     * @see com.pshakhlovich.jackpot.domain.strategy.contribution.VariableDecayContributionStrategy
     */
    VARIABLE_DECAY
}
