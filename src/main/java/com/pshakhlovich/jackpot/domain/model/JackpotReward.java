package com.pshakhlovich.jackpot.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain entity representing a jackpot reward payout to a winning bet.
 * <p>
 * This entity is persisted only when a bet wins the jackpot, recording
 * the payout details, the probability at the time of winning, and the
 * strategy used for evaluation. This provides an audit trail of all
 * jackpot wins for compliance, analytics, and player verification.
 * </p>
 * <p>
 * <strong>Purpose:</strong>
 * </p>
 * <ul>
 *   <li>Compliance - Auditable record of all jackpot wins and payouts</li>
 *   <li>Analytics - Track win frequency, payout amounts, and strategy effectiveness</li>
 *   <li>Player verification - Players can verify their wins and payout amounts</li>
 *   <li>Business intelligence - Analyze jackpot performance and optimize strategies</li>
 * </ul>
 * <p>
 * <strong>Note:</strong> This record is only created for wins. Losing evaluations
 * are not persisted to minimize database writes.
 * </p>
 *
 * @see Jackpot
 * @see RewardStrategyType
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jackpot_reward")
public class JackpotReward {

    /**
     * Unique system-generated identifier for this reward record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reward_id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /**
     * The ID of the bet that won this jackpot reward.
     * <p>
     * Links the reward back to the original bet for player verification
     * and audit purposes.
     * </p>
     */
    @Column(name = "bet_id", nullable = false, length = 64)
    private String betId;

    /**
     * The jackpot that paid out this reward.
     * <p>
     * Lazy-loaded to avoid unnecessary queries when only reward
     * details are needed.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jackpot_id")
    private Jackpot jackpot;

    /**
     * The payout amount awarded to the player.
     * <p>
     * This is the lesser of the current pool and the reward cap at the time
     * of winning. The jackpot pool was reset after this payout.
     * </p>
     */
    @Column(name = "payout_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal payoutAmount;

    /**
     * The probability of winning at the time of evaluation.
     * <p>
     * For fixed strategies, this is constant. For variable strategies, this
     * shows the probability that applied when this bet won, which is useful
     * for analytics and fairness verification.
     * </p>
     */
    @Column(name = "probability", nullable = false, precision = 8, scale = 6)
    private BigDecimal probability;

    /**
     * The reward strategy that was used to evaluate this win.
     * <p>
     * Recorded for audit purposes and to analyze the effectiveness and
     * fairness of different reward strategies.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 32)
    private RewardStrategyType strategy;

    /**
     * Timestamp when this reward was awarded and recorded.
     * <p>
     * Automatically set when the entity is first persisted.
     * </p>
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * JPA lifecycle callback that initializes the creation timestamp.
     */
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
