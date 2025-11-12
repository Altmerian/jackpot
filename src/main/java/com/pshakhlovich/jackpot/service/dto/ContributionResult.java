package com.pshakhlovich.jackpot.service.dto;

import java.math.BigDecimal;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;

/**
 * Data transfer object containing the result of a jackpot contribution calculation.
 * <p>
 * This immutable record encapsulates all information about a contribution that was
 * applied to a jackpot pool, including the strategy used, the calculated contribution
 * amount, and the resulting pool size.
 * </p>
 *
 * @param strategy the contribution strategy that was applied
 * @param contributionAmount the calculated amount contributed to the jackpot pool (scaled to 2 decimal places)
 * @param updatedPool the jackpot pool size after applying the contribution
 */
public record ContributionResult(
        ContributionStrategyType strategy,
        BigDecimal contributionAmount,
        BigDecimal updatedPool
) {
}
