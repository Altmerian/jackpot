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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jackpot")
public class Jackpot {

    @Id
    @Column(name = "jackpot_id", nullable = false, updatable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "initial_pool", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialPool;

    @Column(name = "current_pool", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPool;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_strategy", nullable = false, length = 32)
    private ContributionStrategyType contributionStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_strategy", nullable = false, length = 32)
    private RewardStrategyType rewardStrategy;

    @Column(name = "contribution_rate", precision = 8, scale = 6)
    private BigDecimal contributionRate;

    @Column(name = "min_contribution_rate", precision = 8, scale = 6)
    private BigDecimal minContributionRate;

    @Column(name = "decay_threshold", precision = 19, scale = 2)
    private BigDecimal decayThreshold;

    @Column(name = "decay_slope", precision = 8, scale = 6)
    private BigDecimal decaySlope;

    @Column(name = "reward_base_probability", precision = 8, scale = 6)
    private BigDecimal rewardBaseProbability;

    @Column(name = "reward_max_probability", precision = 8, scale = 6)
    private BigDecimal rewardMaxProbability;

    @Column(name = "reward_ramp_rate", precision = 8, scale = 6)
    private BigDecimal rewardRampRate;

    @Column(name = "reward_cap", precision = 19, scale = 2)
    private BigDecimal rewardCap;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BigDecimal increasePool(BigDecimal delta) {
        currentPool = currentPool.add(delta);
        return currentPool;
    }

    public void resetPoolToInitial() {
        currentPool = initialPool;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
