package com.pshakhlovich.jackpot.domain.model;

/**
 * Defines the available strategies for determining jackpot reward probability and payouts.
 * <p>
 * Each strategy implements different logic for calculating the probability of winning
 * and the corresponding payout amount based on jackpot state.
 * </p>
 */
public enum RewardStrategyType {

    /**
     * Fixed probability reward strategy.
     * <p>
     * Uses a constant probability for winning the jackpot, regardless of
     * the current pool size. The payout is capped at a configured maximum.
     * </p>
     *
     * @see com.pshakhlovich.jackpot.domain.strategy.reward.FixedRewardStrategy
     */
    FIXED,

    /**
     * Variable ramp reward strategy.
     * <p>
     * Uses a probability that increases as the jackpot pool grows, starting from
     * a base probability and ramping up to a maximum (potentially 100%) when the
     * pool reaches a configured cap. This creates urgency as the pool fills.
     * </p>
     *
     * @see com.pshakhlovich.jackpot.domain.strategy.reward.VariableRampRewardStrategy
     */
    VARIABLE_RAMP
}
