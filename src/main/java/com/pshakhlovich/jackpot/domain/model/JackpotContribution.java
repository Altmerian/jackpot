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
 * Domain entity representing a recorded contribution from a bet to a jackpot pool.
 * <p>
 * Each time a bet contributes to a jackpot, a contribution record is persisted
 * to maintain an audit trail. This entity captures the bet details, the calculated
 * contribution amount, the strategy used, and the resulting pool state.
 * </p>
 * <p>
 * <strong>Purpose:</strong>
 * </p>
 * <ul>
 *   <li>Audit trail - Track all contributions for compliance and debugging</li>
 *   <li>Analytics - Analyze contribution patterns and strategy effectiveness</li>
 *   <li>Idempotency - Verify if a bet has already contributed (via betId)</li>
 *   <li>History - Reconstruct jackpot pool growth over time</li>
 * </ul>
 *
 * @see Jackpot
 * @see ContributionStrategyType
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jackpot_contribution")
public class JackpotContribution {

    /**
     * Unique system-generated identifier for this contribution record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contribution_id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /**
     * The ID of the bet that made this contribution.
     * <p>
     * Used for idempotency checks to prevent duplicate contributions
     * if a bet message is reprocessed.
     * </p>
     */
    @Column(name = "bet_id", nullable = false, length = 64)
    private String betId;

    /**
     * The jackpot that received this contribution.
     * <p>
     * Lazy-loaded to avoid unnecessary queries when only contribution
     * details are needed.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jackpot_id")
    private Jackpot jackpot;

    /**
     * The original bet amount that this contribution was calculated from.
     * <p>
     * This is the stake amount, not the contribution itself. Used for
     * analytics and to reconstruct contribution calculations.
     * </p>
     */
    @Column(name = "bet_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal betAmount;

    /**
     * The calculated contribution amount added to the jackpot pool.
     * <p>
     * This is typically a percentage of the bet amount, calculated
     * according to the strategy. The actual amount added to the pool.
     * </p>
     */
    @Column(name = "contribution_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal contributionAmount;

    /**
     * The jackpot pool value after this contribution was applied.
     * <p>
     * Provides a snapshot of the pool state at the time of contribution,
     * enabling pool growth reconstruction and validation.
     * </p>
     */
    @Column(name = "post_contribution_pool", nullable = false, precision = 19, scale = 2)
    private BigDecimal postContributionPool;

    /**
     * The contribution strategy that was used to calculate this contribution.
     * <p>
     * Recorded for audit purposes and to analyze the effectiveness of
     * different contribution strategies.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 32)
    private ContributionStrategyType strategy;

    /**
     * Timestamp when this contribution was recorded.
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
