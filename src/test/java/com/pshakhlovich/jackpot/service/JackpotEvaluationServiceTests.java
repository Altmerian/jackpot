package com.pshakhlovich.jackpot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.JackpotContribution;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.domain.strategy.StrategyRegistry;
import com.pshakhlovich.jackpot.domain.strategy.contribution.FixedContributionStrategy;
import com.pshakhlovich.jackpot.domain.strategy.contribution.VariableDecayContributionStrategy;
import com.pshakhlovich.jackpot.domain.strategy.reward.FixedRewardStrategy;
import com.pshakhlovich.jackpot.domain.strategy.reward.VariableRampRewardStrategy;
import com.pshakhlovich.jackpot.repository.JackpotContributionRepository;
import com.pshakhlovich.jackpot.repository.JackpotRepository;
import com.pshakhlovich.jackpot.repository.JackpotRewardRepository;
import com.pshakhlovich.jackpot.service.dto.RewardResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JackpotEvaluationServiceTests {

    @Mock
    private JackpotContributionRepository contributionRepository;

    @Mock
    private JackpotRepository jackpotRepository;

    @Mock
    private JackpotRewardRepository rewardRepository;

    private JackpotEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        StrategyRegistry strategyRegistry = new StrategyRegistry(
                List.of(new FixedContributionStrategy(), new VariableDecayContributionStrategy()),
                List.of(new FixedRewardStrategy(), new VariableRampRewardStrategy()));

        evaluationService = new JackpotEvaluationService(
                contributionRepository,
                jackpotRepository,
                rewardRepository,
                strategyRegistry);
    }

    @Test
    void shouldReturnWinWithFixedStrategyWhenRandomDrawBelowProbability() {
        // Given: Fixed strategy with very high probability to ensure win
        String betId = "bet-win-fixed";
        String jackpotId = "fixed-warmup";

        Jackpot jackpot = baseJackpot()
                .id(jackpotId)
                .rewardStrategy(RewardStrategyType.FIXED)
                .rewardBaseProbability(new BigDecimal("1.000000")) // 100% probability
                .currentPool(new BigDecimal("1200.00"))
                .rewardCap(new BigDecimal("1000.00"))
                .build();

        JackpotContribution contribution = createContribution(betId, jackpot);

        when(contributionRepository.findByBetIdAndJackpotId(betId, jackpotId))
                .thenReturn(Optional.of(contribution));
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.of(jackpot));

        // When: Evaluate (deterministic RNG with high probability will produce win)
        RewardResult result = evaluationService.evaluate(betId, jackpotId);

        // Then: Verify deterministic outcome
        assertThat(result.win()).isTrue();
        assertThat(result.payoutAmount()).isEqualByComparingTo("1000.00"); // Capped at rewardCap
        assertThat(result.updatedPool()).isEqualByComparingTo("500.00"); // Reset to initial
        assertThat(result.strategy()).isEqualTo(RewardStrategyType.FIXED);
        assertThat(result.probability()).isEqualByComparingTo("1.000000");

        verify(rewardRepository).save(any());
        verify(jackpotRepository).save(jackpot);
    }

    @Test
    void shouldReturnNoWinWithFixedStrategyWhenRandomDrawAboveProbability() {
        // Given: Fixed strategy with low probability
        String betId = "bet-no-win-fixed";
        String jackpotId = "fixed-warmup";

        Jackpot jackpot = baseJackpot()
                .id(jackpotId)
                .rewardStrategy(RewardStrategyType.FIXED)
                .rewardBaseProbability(new BigDecimal("0.000000")) // 0% probability
                .currentPool(new BigDecimal("800.00"))
                .rewardCap(new BigDecimal("1000.00"))
                .build();

        JackpotContribution contribution = createContribution(betId, jackpot);

        when(contributionRepository.findByBetIdAndJackpotId(betId, jackpotId))
                .thenReturn(Optional.of(contribution));
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.of(jackpot));

        // When: Evaluate (deterministic RNG will produce value > 0.000001)
        RewardResult result = evaluationService.evaluate(betId, jackpotId);

        // Then: No win
        assertThat(result.win()).isFalse();
        assertThat(result.payoutAmount()).isEqualByComparingTo("0.00");
        assertThat(result.updatedPool()).isEqualByComparingTo("800.00"); // Pool unchanged
        assertThat(result.strategy()).isEqualTo(RewardStrategyType.FIXED);

        verify(rewardRepository, never()).save(any());
        verify(jackpotRepository, never()).save(any());
    }

    @Test
    void shouldReturnWinWithVariableRampStrategyWhenPoolNearCap() {
        // Given: Variable ramp strategy with pool near cap (high probability)
        String betId = "bet-win-ramp";
        String jackpotId = "ramp-jackpot";

        Jackpot jackpot = baseJackpot()
                .id(jackpotId)
                .rewardStrategy(RewardStrategyType.VARIABLE_RAMP)
                .rewardBaseProbability(new BigDecimal("0.01"))
                .rewardMaxProbability(new BigDecimal("1.00"))
                .rewardRampRate(new BigDecimal("0.99"))
                .currentPool(new BigDecimal("9800.00"))
                .rewardCap(new BigDecimal("10000.00"))
                .build();

        JackpotContribution contribution = createContribution(betId, jackpot);

        when(contributionRepository.findByBetIdAndJackpotId(betId, jackpotId))
                .thenReturn(Optional.of(contribution));
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.of(jackpot));

        // When: Evaluate (high pool ratio -> high probability)
        RewardResult result = evaluationService.evaluate(betId, jackpotId);

        // Then: Win expected due to high probability
        assertThat(result.win()).isTrue();
        assertThat(result.payoutAmount()).isEqualByComparingTo("9800.00");
        assertThat(result.updatedPool()).isEqualByComparingTo("500.00"); // Reset
        assertThat(result.strategy()).isEqualTo(RewardStrategyType.VARIABLE_RAMP);
        assertThat(result.probability().doubleValue()).isGreaterThan(0.95); // High probability

        verify(rewardRepository).save(any());
        verify(jackpotRepository).save(jackpot);
    }

    @Test
    void shouldReturnNoWinWithVariableRampStrategyWhenPoolLow() {
        // Given: Variable ramp with low pool (low probability)
        String betId = "bet-no-win-ramp";
        String jackpotId = "ramp-jackpot";

        Jackpot jackpot = baseJackpot()
                .id(jackpotId)
                .rewardStrategy(RewardStrategyType.VARIABLE_RAMP)
                .rewardBaseProbability(new BigDecimal("0.000000"))
                .rewardMaxProbability(new BigDecimal("0.50"))
                .rewardRampRate(new BigDecimal("0.10"))
                .currentPool(new BigDecimal("600.00"))
                .rewardCap(new BigDecimal("10000.00"))
                .build();

        JackpotContribution contribution = createContribution(betId, jackpot);

        when(contributionRepository.findByBetIdAndJackpotId(betId, jackpotId))
                .thenReturn(Optional.of(contribution));
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.of(jackpot));

        // When: Evaluate (low pool ratio -> low probability)
        RewardResult result = evaluationService.evaluate(betId, jackpotId);

        // Then: No win due to low probability
        assertThat(result.win()).isFalse();
        assertThat(result.payoutAmount()).isEqualByComparingTo("0.00");
        assertThat(result.updatedPool()).isEqualByComparingTo("600.00");
        assertThat(result.strategy()).isEqualTo(RewardStrategyType.VARIABLE_RAMP);

        verify(rewardRepository, never()).save(any());
        verify(jackpotRepository, never()).save(any());
    }

    @Test
    void shouldProduceDeterministicOutcomeForSameBetIdAndJackpotId() {
        // Given: Same betId and jackpotId
        String betId = "bet-deterministic";
        String jackpotId = "fixed-warmup";

        Jackpot jackpot = baseJackpot()
                .id(jackpotId)
                .rewardStrategy(RewardStrategyType.FIXED)
                .rewardBaseProbability(new BigDecimal("0.05"))
                .currentPool(new BigDecimal("1000.00"))
                .build();

        JackpotContribution contribution = createContribution(betId, jackpot);

        when(contributionRepository.findByBetIdAndJackpotId(betId, jackpotId))
                .thenReturn(Optional.of(contribution));
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.of(jackpot));

        // When: Evaluate multiple times
        RewardResult result1 = evaluationService.evaluate(betId, jackpotId);

        // Reset jackpot pool for second evaluation (simulating same initial state)
        jackpot.setCurrentPool(new BigDecimal("1000.00"));
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.of(jackpot));

        RewardResult result2 = evaluationService.evaluate(betId, jackpotId);

        // Then: Same outcome due to deterministic RNG
        assertThat(result1.win()).isEqualTo(result2.win());
        assertThat(result1.probability()).isEqualByComparingTo(result2.probability());
    }

    @Test
    void shouldThrowExceptionWhenContributionNotFound() {
        // Given: No contribution exists
        String betId = "non-existent-bet";
        String jackpotId = "fixed-warmup";

        when(contributionRepository.findByBetIdAndJackpotId(betId, jackpotId))
                .thenReturn(Optional.empty());

        // When/Then: Exception thrown
        assertThatThrownBy(() -> evaluationService.evaluate(betId, jackpotId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No contribution found");
    }

    @Test
    void shouldThrowExceptionWhenJackpotNotFound() {
        // Given: Contribution exists but jackpot doesn't
        String betId = "bet-123";
        String jackpotId = "non-existent-jackpot";

        Jackpot dummyJackpot = baseJackpot().id("dummy").build();
        JackpotContribution contribution = createContribution(betId, dummyJackpot);

        when(contributionRepository.findByBetIdAndJackpotId(betId, jackpotId))
                .thenReturn(Optional.of(contribution));
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.empty());

        // When/Then: Exception thrown
        assertThatThrownBy(() -> evaluationService.evaluate(betId, jackpotId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Jackpot")
                .hasMessageContaining("not found");
    }

    private Jackpot.JackpotBuilder baseJackpot() {
        return Jackpot.builder()
                .id("fixed-warmup")
                .name("Fixed Warmup")
                .initialPool(new BigDecimal("500.00"))
                .currentPool(new BigDecimal("500.00"))
                .contributionStrategy(ContributionStrategyType.FIXED_RATE)
                .contributionRate(new BigDecimal("0.10"))
                .rewardStrategy(RewardStrategyType.FIXED)
                .rewardBaseProbability(new BigDecimal("0.05"))
                .rewardMaxProbability(new BigDecimal("0.05"))
                .rewardRampRate(BigDecimal.ZERO)
                .rewardCap(new BigDecimal("1000.00"));
    }

    private JackpotContribution createContribution(String betId, Jackpot jackpot) {
        return JackpotContribution.builder()
                .betId(betId)
                .jackpot(jackpot)
                .betAmount(new BigDecimal("100.00"))
                .contributionAmount(new BigDecimal("10.00"))
                .postContributionPool(jackpot.getCurrentPool())
                .strategy(ContributionStrategyType.FIXED_RATE)
                .build();
    }
}
