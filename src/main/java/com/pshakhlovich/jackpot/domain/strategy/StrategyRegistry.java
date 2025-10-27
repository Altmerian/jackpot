package com.pshakhlovich.jackpot.domain.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.domain.strategy.contribution.ContributionStrategy;
import com.pshakhlovich.jackpot.domain.strategy.reward.RewardStrategy;

@Component
public class StrategyRegistry {

    private final Map<ContributionStrategyType, ContributionStrategy> contributionStrategies;
    private final Map<RewardStrategyType, RewardStrategy> rewardStrategies;

    public StrategyRegistry(List<ContributionStrategy> contributionStrategies,
                            List<RewardStrategy> rewardStrategies) {
        this.contributionStrategies = new EnumMap<>(ContributionStrategyType.class);
        contributionStrategies.forEach(strategy -> this.contributionStrategies.put(strategy.type(), strategy));

        this.rewardStrategies = new EnumMap<>(RewardStrategyType.class);
        rewardStrategies.forEach(strategy -> this.rewardStrategies.put(strategy.type(), strategy));
    }

    public ContributionStrategy getContributionStrategy(ContributionStrategyType type) {
        ContributionStrategy strategy = contributionStrategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No contribution strategy registered for type: " + type);
        }
        return strategy;
    }

    public RewardStrategy getRewardStrategy(RewardStrategyType type) {
        RewardStrategy strategy = rewardStrategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No reward strategy registered for type: " + type);
        }
        return strategy;
    }

}
