package com.pshakhlovich.jackpot.domain.strategy.contribution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.service.dto.ContributionResult;

@Component
public class FixedContributionStrategy implements ContributionStrategy {

    @Override
    public ContributionStrategyType type() {
        return ContributionStrategyType.FIXED_RATE;
    }

    @Override
    public ContributionResult contribute(Jackpot jackpot, BigDecimal betAmount) {
        BigDecimal rate = jackpot.getContributionRate();
        if (rate == null) {
            throw new IllegalStateException("Fixed contribution strategy requires contributionRate configuration");
        }

        BigDecimal contribution = betAmount.multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal updatedPool = jackpot.increasePool(contribution);

        return new ContributionResult(
                ContributionStrategyType.FIXED_RATE,
                contribution,
                updatedPool);
    }
}
