package com.pshakhlovich.jackpot.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain entity representing a jackpot with configurable contribution and reward strategies.
 * <p>
 * A jackpot maintains a growing pool of money that accumulates from bet contributions
 * and pays out rewards based on configured probability. Each jackpot can be configured
 * with different strategies for both contribution calculation and reward evaluation,
 * enabling flexible and diverse jackpot mechanics.
 * </p>
 * <p>
 * <strong>Lifecycle:</strong>
 * </p>
 * <ol>
 *   <li>Jackpot is created with an initial pool and strategy configuration</li>
 *   <li>Bets contribute to the pool based on the contribution strategy</li>
 *   <li>Bets are evaluated for rewards based on the reward strategy</li>
 *   <li>When won, the pool resets to the initial value and the cycle repeats</li>
 * </ol>
 * <p>
 * <strong>Strategy Configuration:</strong> Different fields are required depending on
 * the selected strategies. See {@link ContributionStrategyType} and {@link RewardStrategyType}
 * for details on which fields are required for each strategy.
 * </p>
 *
 * @see ContributionStrategyType
 * @see RewardStrategyType
 * @see JackpotContribution
 * @see JackpotReward
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jackpot")
public class Jackpot {

    /**
     * Unique identifier for this jackpot.
     * <p>
     * This ID is used to match bets to the correct jackpot and is immutable once set.
     * </p>
     */
    @Id
    @Column(name = "jackpot_id", nullable = false, updatable = false, length = 64)
    private String id;

    /**
     * Human-readable name for display purposes.
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * The starting pool value that the jackpot resets to after being won.
     * <p>
     * This provides a guaranteed minimum pool for the next winner and ensures
     * the jackpot remains attractive even after a payout.
     * </p>
     */
    @Column(name = "initial_pool", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialPool;

    /**
     * The current accumulated pool value.
     * <p>
     * This value increases with each bet contribution and resets to {@link #initialPool}
     * when the jackpot is won.
     * </p>
     */
    @Column(name = "current_pool", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPool;

    /**
     * The strategy used to calculate contribution amounts from bet amounts.
     *
     * @see ContributionStrategyType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_strategy", nullable = false, length = 32)
    private ContributionStrategyType contributionStrategy;

    /**
     * The strategy used to evaluate reward eligibility and calculate payouts.
     *
     * @see RewardStrategyType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reward_strategy", nullable = false, length = 32)
    private RewardStrategyType rewardStrategy;

    // === Contribution Strategy Configuration Fields ===

    /**
     * The base/fixed contribution rate as a decimal (e.g., 0.05 for 5%).
     * <p>
     * <strong>Required for:</strong> FIXED_RATE, VARIABLE_DECAY
     * </p>
     */
    @Column(name = "contribution_rate", precision = 8, scale = 6)
    private BigDecimal contributionRate;

    /**
     * The minimum contribution rate for variable strategies (floor).
     * <p>
     * <strong>Required for:</strong> VARIABLE_DECAY
     * </p>
     */
    @Column(name = "min_contribution_rate", precision = 8, scale = 6)
    private BigDecimal minContributionRate;

    /**
     * The pool size threshold at which variable contribution reaches minimum rate.
     * <p>
     * <strong>Required for:</strong> VARIABLE_DECAY
     * </p>
     */
    @Column(name = "decay_threshold", precision = 19, scale = 2)
    private BigDecimal decayThreshold;

    /**
     * How much the contribution rate decreases per unit of pool ratio.
     * <p>
     * Typically set to (contributionRate - minContributionRate) for linear decay.
     * <strong>Required for:</strong> VARIABLE_DECAY
     * </p>
     */
    @Column(name = "decay_slope", precision = 8, scale = 6)
    private BigDecimal decaySlope;

    // === Reward Strategy Configuration Fields ===

    /**
     * The base/minimum probability of winning the jackpot (e.g., 0.01 for 1%).
     * <p>
     * <strong>Required for:</strong> FIXED, VARIABLE_RAMP
     * </p>
     */
    @Column(name = "reward_base_probability", precision = 8, scale = 6)
    private BigDecimal rewardBaseProbability;

    /**
     * The maximum probability ceiling (e.g., 1.0 for 100%).
     * <p>
     * <strong>Required for:</strong> VARIABLE_RAMP
     * </p>
     */
    @Column(name = "reward_max_probability", precision = 8, scale = 6)
    private BigDecimal rewardMaxProbability;

    /**
     * How much the probability increases per unit of pool ratio.
     * <p>
     * Typically set to (maxProbability - baseProbability) for linear ramp.
     * <strong>Required for:</strong> VARIABLE_RAMP
     * </p>
     */
    @Column(name = "reward_ramp_rate", precision = 8, scale = 6)
    private BigDecimal rewardRampRate;

    /**
     * The maximum payout amount (caps the pool value for payouts).
     * <p>
     * Also used as the threshold for variable probability calculation in VARIABLE_RAMP.
     * <strong>Required for:</strong> FIXED, VARIABLE_RAMP
     * </p>
     */
    @Column(name = "reward_cap", precision = 19, scale = 2)
    private BigDecimal rewardCap;

    /**
     * Timestamp when this jackpot was first created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Timestamp of the last update to this jackpot.
     * <p>
     * Automatically updated on every modification via {@link #onUpdate()}.
     * </p>
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Increases the current pool by the specified amount.
     * <p>
     * This method is called by contribution strategies to update the pool
     * after calculating the contribution amount.
     * </p>
     * <p>
     * <strong>Concurrency:</strong> This method must be called within a transaction
     * with pessimistic locking to prevent race conditions.
     * </p>
     *
     * @param delta the amount to add to the pool (must be non-negative)
     * @return the new pool value after increase
     */
    public BigDecimal increasePool(BigDecimal delta) {
        currentPool = currentPool.add(delta);
        return currentPool;
    }

    /**
     * Resets the current pool to the initial pool value.
     * <p>
     * This method is called by reward strategies when the jackpot is won,
     * starting a new jackpot cycle with the guaranteed minimum pool.
     * </p>
     */
    public void resetPoolToInitial() {
        currentPool = initialPool;
    }

    /**
     * JPA lifecycle callback that initializes timestamps when the entity is first persisted.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * JPA lifecycle callback that updates the modification timestamp on every update.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
