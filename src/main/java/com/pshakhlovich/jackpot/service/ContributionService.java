package com.pshakhlovich.jackpot.service;

import com.pshakhlovich.jackpot.avro.Bet;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.strategy.StrategyRegistry;
import com.pshakhlovich.jackpot.domain.strategy.contribution.ContributionStrategy;
import com.pshakhlovich.jackpot.repository.JackpotContributionRepository;
import com.pshakhlovich.jackpot.repository.JackpotRepository;
import com.pshakhlovich.jackpot.service.dto.ContributionResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionService {

    private final JackpotRepository jackpotRepository;
    private final JackpotContributionRepository contributionRepository;
    private final StrategyRegistry strategyRegistry;

    @Transactional("transactionManager")
    public void applyContribution(Bet bet) {
        BigDecimal betAmount = BigDecimal.valueOf(bet.getBetAmount()).setScale(2, RoundingMode.HALF_UP);

        // Load jackpot with pessimistic write lock to ensure exclusive access during pool updates
        // This prevents race conditions in concurrent contribution processing
        Jackpot jackpot = jackpotRepository.findByIdForUpdate(bet.getJackpotId())
                .orElseThrow(() -> new IllegalArgumentException("Jackpot %s not found".formatted(bet.getJackpotId())));

        ContributionStrategy strategy = strategyRegistry.getContributionStrategy(jackpot.getContributionStrategy());
        ContributionResult result = strategy.contribute(jackpot, betAmount);

        contributionRepository.save(com.pshakhlovich.jackpot.domain.model.JackpotContribution.builder()
                .betId(bet.getBetId())
                .jackpot(jackpot)
                .betAmount(betAmount)
                .contributionAmount(result.contributionAmount())
                .postContributionPool(result.updatedPool())
                .strategy(result.strategy())
                .build());

        jackpotRepository.save(jackpot);

        BigDecimal effectiveRate = betAmount.signum() > 0
                ? result.contributionAmount().divide(betAmount, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("Applied contribution: betId={}, jackpotId={}, strategy={}, contribution={}, pool={}, effectiveRate={}",
                bet.getBetId(),
                bet.getJackpotId(),
                result.strategy(),
                result.contributionAmount(),
                result.updatedPool(),
                effectiveRate);
    }
}
