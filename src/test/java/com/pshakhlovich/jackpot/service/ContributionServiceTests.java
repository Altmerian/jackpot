package com.pshakhlovich.jackpot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pshakhlovich.jackpot.avro.Bet;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.domain.strategy.StrategyRegistry;
import com.pshakhlovich.jackpot.domain.strategy.contribution.FixedContributionStrategy;
import com.pshakhlovich.jackpot.domain.strategy.contribution.VariableDecayContributionStrategy;
import com.pshakhlovich.jackpot.domain.strategy.reward.FixedRewardStrategy;
import com.pshakhlovich.jackpot.domain.strategy.reward.VariableRampRewardStrategy;
import com.pshakhlovich.jackpot.repository.JackpotContributionRepository;
import com.pshakhlovich.jackpot.repository.JackpotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTests {

    @Mock
    private JackpotRepository jackpotRepository;

    @Mock
    private JackpotContributionRepository contributionRepository;

    private ContributionService contributionService;

    @BeforeEach
    void setUp() {
        StrategyRegistry strategyRegistry = new StrategyRegistry(
                List.of(new FixedContributionStrategy(), new VariableDecayContributionStrategy()),
                List.of(new FixedRewardStrategy(), new VariableRampRewardStrategy()));

        contributionService = new ContributionService(jackpotRepository, contributionRepository, strategyRegistry);
    }

    @Test
    void shouldPersistContributionUsingFixedStrategy() {
        Jackpot jackpot = baseJackpot()
                .contributionStrategy(ContributionStrategyType.FIXED_RATE)
                .contributionRate(new BigDecimal("0.10"))
                .currentPool(new BigDecimal("500.00"))
                .build();

        when(jackpotRepository.findById("fixed-warmup")).thenReturn(Optional.of(jackpot));

        Bet bet = Bet.newBuilder()
                .setBetId("bet-123")
                .setJackpotId("fixed-warmup")
                .setUserId("user-1")
                .setBetAmount(200.0)
                .setCreatedAt(Instant.now())
                .build();

        contributionService.applyContribution(bet);

        ArgumentCaptor<com.pshakhlovich.jackpot.domain.model.JackpotContribution> captor = ArgumentCaptor.forClass(com.pshakhlovich.jackpot.domain.model.JackpotContribution.class);
        verify(contributionRepository).save(captor.capture());
        com.pshakhlovich.jackpot.domain.model.JackpotContribution saved = captor.getValue();

        assertThat(saved.getContributionAmount()).isEqualByComparingTo("20.00");
        assertThat(saved.getPostContributionPool()).isEqualByComparingTo("520.00");
        assertThat(saved.getStrategy()).isEqualTo(ContributionStrategyType.FIXED_RATE);
        assertThat(jackpot.getCurrentPool()).isEqualByComparingTo("520.00");

        verify(jackpotRepository).save(jackpot);
    }

    @Test
    void shouldPersistContributionUsingVariableDecayStrategy() {
        Jackpot jackpot = baseJackpot()
                .id("decaying-marathon")
                .contributionStrategy(ContributionStrategyType.VARIABLE_DECAY)
                .contributionRate(new BigDecimal("0.12"))
                .minContributionRate(new BigDecimal("0.04"))
                .decaySlope(new BigDecimal("0.08"))
                .decayThreshold(new BigDecimal("10000.00"))
                .currentPool(new BigDecimal("2000.00"))
                .build();

        when(jackpotRepository.findById("decaying-marathon")).thenReturn(Optional.of(jackpot));

        Bet bet = Bet.newBuilder()
                .setBetId("bet-456")
                .setJackpotId("decaying-marathon")
                .setUserId("user-2")
                .setBetAmount(150.0)
                .setCreatedAt(Instant.now())
                .build();

        contributionService.applyContribution(bet);

        ArgumentCaptor<com.pshakhlovich.jackpot.domain.model.JackpotContribution> captor = ArgumentCaptor.forClass(com.pshakhlovich.jackpot.domain.model.JackpotContribution.class);
        verify(contributionRepository).save(captor.capture());
        com.pshakhlovich.jackpot.domain.model.JackpotContribution saved = captor.getValue();

        assertThat(saved.getStrategy()).isEqualTo(ContributionStrategyType.VARIABLE_DECAY);
        assertThat(saved.getContributionAmount()).isEqualByComparingTo("15.60");
        assertThat(saved.getPostContributionPool()).isEqualByComparingTo("2015.60");

        verify(jackpotRepository).save(jackpot);
    }

    private Jackpot.JackpotBuilder baseJackpot() {
        return Jackpot.builder()
                .id("fixed-warmup")
                .name("Fixed Warmup")
                .initialPool(new BigDecimal("500.00"))
                .currentPool(new BigDecimal("500.00"))
                .rewardStrategy(RewardStrategyType.FIXED)
                .rewardBaseProbability(new BigDecimal("0.05"))
                .rewardMaxProbability(new BigDecimal("0.05"))
                .rewardRampRate(BigDecimal.ZERO)
                .rewardCap(new BigDecimal("1000.00"));
    }
}
