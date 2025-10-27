package com.pshakhlovich.jackpot.service.dto;

import java.math.BigDecimal;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;

public record ContributionResult(
        ContributionStrategyType strategy,
        BigDecimal contributionAmount,
        BigDecimal updatedPool
) {
}
