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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jackpot_contribution")
public class JackpotContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contribution_id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "bet_id", nullable = false, length = 64)
    private String betId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jackpot_id")
    private Jackpot jackpot;

    @Column(name = "bet_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal betAmount;

    @Column(name = "contribution_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal contributionAmount;

    @Column(name = "post_contribution_pool", nullable = false, precision = 19, scale = 2)
    private BigDecimal postContributionPool;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 32)
    private ContributionStrategyType strategy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
