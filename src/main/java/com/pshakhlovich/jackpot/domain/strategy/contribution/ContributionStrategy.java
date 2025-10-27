package com.pshakhlovich.jackpot.domain.strategy.contribution;

import java.math.BigDecimal;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.service.dto.ContributionResult;

public interface ContributionStrategy {

    ContributionStrategyType type();

    ContributionResult contribute(Jackpot jackpot, BigDecimal betAmount);
}
