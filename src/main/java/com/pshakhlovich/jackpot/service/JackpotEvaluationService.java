package com.pshakhlovich.jackpot.service;

import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.JackpotContribution;
import com.pshakhlovich.jackpot.domain.model.JackpotReward;
import com.pshakhlovich.jackpot.domain.strategy.StrategyRegistry;
import com.pshakhlovich.jackpot.domain.strategy.reward.RewardStrategy;
import com.pshakhlovich.jackpot.repository.JackpotContributionRepository;
import com.pshakhlovich.jackpot.repository.JackpotRepository;
import com.pshakhlovich.jackpot.repository.JackpotRewardRepository;
import com.pshakhlovich.jackpot.service.dto.RewardResult;
import com.pshakhlovich.jackpot.support.DeterministicRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JackpotEvaluationService {

    private final JackpotContributionRepository contributionRepository;
    private final JackpotRepository jackpotRepository;
    private final JackpotRewardRepository rewardRepository;
    private final StrategyRegistry strategyRegistry;

    @Transactional(transactionManager = "transactionManager")
    public RewardResult evaluate(String betId, String jackpotId) {
        // Verify bet contribution exists
        JackpotContribution contribution = contributionRepository.findByBetIdAndJackpotId(betId, jackpotId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No contribution found for betId=%s and jackpotId=%s".formatted(betId, jackpotId)));

        // Load jackpot
        Jackpot jackpot = jackpotRepository.findById(jackpotId)
                .orElseThrow(() -> new IllegalArgumentException("Jackpot %s not found".formatted(jackpotId)));

        // Generate deterministic random draw
        DeterministicRandom random = new DeterministicRandom(betId + jackpotId);
        double randomDraw = random.nextDouble();

        // Apply reward strategy
        RewardStrategy strategy = strategyRegistry.getRewardStrategy(jackpot.getRewardStrategy());
        RewardResult result = strategy.evaluate(jackpot, randomDraw);

        // Persist reward and save updated jackpot state if win
        if (result.win()) {
            JackpotReward reward = JackpotReward.builder()
                    .betId(betId)
                    .jackpot(jackpot)
                    .payoutAmount(result.payoutAmount())
                    .probability(result.probability())
                    .strategy(result.strategy())
                    .build();

            rewardRepository.save(reward);
            jackpotRepository.save(jackpot);

            log.info("Jackpot reward paid: betId={}, jackpotId={}, strategy={}, probability={}, payout={}, updatedPool={}",
                    betId,
                    jackpotId,
                    result.strategy(),
                    result.probability(),
                    result.payoutAmount(),
                    result.updatedPool());
        } else {
            log.debug("Jackpot evaluation - no win: betId={}, jackpotId={}, strategy={}, probability={}, randomDraw={}",
                    betId,
                    jackpotId,
                    result.strategy(),
                    result.probability(),
                    randomDraw);
        }

        return result;
    }
}
