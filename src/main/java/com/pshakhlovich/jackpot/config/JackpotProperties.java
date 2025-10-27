package com.pshakhlovich.jackpot.config;

import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jackpot")
public record JackpotProperties(List<JackpotProfileProperties> profiles) {

    public JackpotProperties {
        profiles = profiles == null ? List.of() : Collections.unmodifiableList(profiles);
    }

    public record JackpotProfileProperties(
            String id,
            String name,
            BigDecimal initialPool,
            ContributionStrategyType contributionStrategy,
            RewardStrategyType rewardStrategy,
            Contribution contribution,
            Reward reward) {
    }

    public record Contribution(
            BigDecimal rate,
            BigDecimal minRate,
            BigDecimal decayThreshold,
            BigDecimal decaySlope) {
    }

    public record Reward(
            BigDecimal baseProbability,
            BigDecimal maxProbability,
            BigDecimal rampRate,
            BigDecimal cap) {
    }
}
