package com.pshakhlovich.jackpot.service.dto;

import java.math.BigDecimal;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;

/**
 * Data transfer object containing the result of a jackpot reward evaluation.
 * <p>
 * This immutable record encapsulates all information about a reward evaluation,
 * including the strategy used, the calculated win probability, whether the bet won,
 * the payout amount (if won), and the resulting pool size after the evaluation.
 * </p>
 *
 * @param strategy the reward strategy that was used for evaluation
 * @param probability the calculated probability of winning at the time of evaluation (scaled to 6 decimal places)
 * @param payoutAmount the payout amount if the bet won, or zero if it lost
 * @param updatedPool the jackpot pool size after the evaluation (reset to initial if won, unchanged if lost)
 * @param win {@code true} if the bet won the jackpot, {@code false} otherwise
 */
public record RewardResult(
        RewardStrategyType strategy,
        BigDecimal probability,
        BigDecimal payoutAmount,
        BigDecimal updatedPool,
        boolean win
) {
}
