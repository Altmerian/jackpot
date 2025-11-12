package com.pshakhlovich.jackpot.domain.strategy.contribution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.service.dto.ContributionResult;

/**
 * Fixed rate contribution strategy implementation.
 * <p>
 * This strategy applies a constant percentage of the bet amount as a contribution
 * to the jackpot pool, regardless of the current pool size. The contribution rate
 * is configured per jackpot via the {@code contributionRate} field.
 * </p>
 * <p>
 * <strong>Formula:</strong> {@code contribution = betAmount * contributionRate}
 * </p>
 * <p>
 * <strong>Example:</strong> If {@code contributionRate = 0.05} (5%) and {@code betAmount = 100.00},
 * then {@code contribution = 5.00}
 * </p>
 * <p>
 * <strong>Required jackpot configuration:</strong>
 * </p>
 * <ul>
 *   <li>{@code contributionRate} - The fixed percentage rate (e.g., 0.05 for 5%)</li>
 * </ul>
 *
 * @see ContributionStrategy
 * @see ContributionStrategyType#FIXED_RATE
 */
@Component
public class FixedContributionStrategy implements ContributionStrategy {

    @Override
    public ContributionStrategyType type() {
        return ContributionStrategyType.FIXED_RATE;
    }

    /**
     * Calculates a fixed percentage contribution from the bet amount.
     *
     * @param jackpot the jackpot to contribute to (must have {@code contributionRate} configured)
     * @param betAmount the bet amount to calculate contribution from
     * @return the contribution result with calculated amount and updated pool
     * @throws IllegalStateException if {@code contributionRate} is not configured
     */
    @Override
    public ContributionResult contribute(Jackpot jackpot, BigDecimal betAmount) {
        // Retrieve the fixed contribution rate from jackpot configuration
        BigDecimal rate = jackpot.getContributionRate();
        if (rate == null) {
            throw new IllegalStateException("Fixed contribution strategy requires contributionRate configuration");
        }

        // Calculate contribution as: betAmount * rate
        BigDecimal contribution = betAmount.multiply(rate)
                .setScale(2, RoundingMode.HALF_UP); // Round to 2 decimal places for currency

        // Update the jackpot pool by adding the contribution
        BigDecimal updatedPool = jackpot.increasePool(contribution);

        return new ContributionResult(
                ContributionStrategyType.FIXED_RATE,
                contribution,
                updatedPool);
    }
}
