package com.pshakhlovich.jackpot.domain.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.domain.strategy.contribution.ContributionStrategy;
import com.pshakhlovich.jackpot.domain.strategy.reward.RewardStrategy;

/**
 * Central registry for looking up contribution and reward strategy implementations.
 * <p>
 * This component acts as a factory/registry that automatically discovers all
 * strategy implementations registered as Spring beans and provides type-safe
 * lookup by strategy type. This enables the Strategy design pattern with automatic
 * registration via Spring's dependency injection.
 * </p>
 * <p>
 * <strong>How it works:</strong>
 * </p>
 * <ol>
 *   <li>Spring discovers all {@link ContributionStrategy} and {@link RewardStrategy}
 *       implementations marked with {@code @Component}</li>
 *   <li>These implementations are injected into the registry constructor as lists</li>
 *   <li>The registry indexes them by their {@code type()} for O(1) lookup</li>
 *   <li>Services can retrieve strategies by type without knowing concrete classes</li>
 * </ol>
 * <p>
 * <strong>Adding new strategies:</strong> Simply create a new class implementing
 * {@link ContributionStrategy} or {@link RewardStrategy}, annotate it with
 * {@code @Component}, and it will be automatically registered.
 * </p>
 *
 * @see ContributionStrategy
 * @see RewardStrategy
 */
@Component
public class StrategyRegistry {

    private final Map<ContributionStrategyType, ContributionStrategy> contributionStrategies;
    private final Map<RewardStrategyType, RewardStrategy> rewardStrategies;

    /**
     * Constructs the registry and indexes all strategy implementations.
     * <p>
     * Spring automatically injects all discovered strategy implementations,
     * and the constructor indexes them by type for efficient lookup.
     * </p>
     *
     * @param contributionStrategies all contribution strategy implementations discovered by Spring
     * @param rewardStrategies all reward strategy implementations discovered by Spring
     */
    public StrategyRegistry(List<ContributionStrategy> contributionStrategies,
                            List<RewardStrategy> rewardStrategies) {
        this.contributionStrategies = new EnumMap<>(ContributionStrategyType.class);

        contributionStrategies.forEach(strategy -> this.contributionStrategies.put(strategy.type(), strategy));

        this.rewardStrategies = new EnumMap<>(RewardStrategyType.class);
        rewardStrategies.forEach(strategy -> this.rewardStrategies.put(strategy.type(), strategy));
    }

    /**
     * Retrieves the contribution strategy implementation for the given type.
     *
     * @param type the strategy type to look up
     * @return the strategy implementation
     * @throws IllegalStateException if no strategy is registered for the given type
     *                               (indicates misconfiguration or missing implementation)
     */
    public ContributionStrategy getContributionStrategy(ContributionStrategyType type) {
        ContributionStrategy strategy = contributionStrategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No contribution strategy registered for type: " + type);
        }
        return strategy;
    }

    /**
     * Retrieves the reward strategy implementation for the given type.
     *
     * @param type the strategy type to look up
     * @return the strategy implementation
     * @throws IllegalStateException if no strategy is registered for the given type
     *                               (indicates misconfiguration or missing implementation)
     */
    public RewardStrategy getRewardStrategy(RewardStrategyType type) {
        RewardStrategy strategy = rewardStrategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No reward strategy registered for type: " + type);
        }
        return strategy;
    }

}
