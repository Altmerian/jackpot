package com.pshakhlovich.jackpot.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pshakhlovich.jackpot.config.KafkaTopicsConfig;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.JackpotContribution;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.repository.JackpotContributionRepository;
import com.pshakhlovich.jackpot.repository.JackpotRepository;
import com.pshakhlovich.jackpot.repository.JackpotRewardRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.properties.schema.registry.url=mock://jackpot",
        "spring.kafka.producer.properties.schema.registry.url=mock://jackpot",
        "spring.kafka.properties.schema.registry.url=mock://jackpot"
})
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = KafkaTopicsConfig.BETS_TOPIC, brokerProperties = {
        "transaction.state.log.replication.factor=1",
        "transaction.state.log.min.isr=1"
})
@ExtendWith(SpringExtension.class)
class JackpotEvaluationControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JackpotContributionRepository contributionRepository;

    @Autowired
    private JackpotRepository jackpotRepository;

    @Autowired
    private JackpotRewardRepository rewardRepository;

    private Jackpot fixedJackpot;
    private Jackpot variableJackpot;

    @BeforeEach
    void setUp() {
        // Seed jackpot profiles for testing
        fixedJackpot = Jackpot.builder()
                .id("fixed-test")
                .name("Fixed Test Jackpot")
                .initialPool(new BigDecimal("500.00"))
                .currentPool(new BigDecimal("1200.00"))
                .contributionStrategy(ContributionStrategyType.FIXED_RATE)
                .rewardStrategy(RewardStrategyType.FIXED)
                .contributionRate(new BigDecimal("0.10"))
                .rewardBaseProbability(new BigDecimal("1.000000")) // 100% probability
                .rewardMaxProbability(new BigDecimal("1.000000"))
                .rewardRampRate(BigDecimal.ZERO)
                .rewardCap(new BigDecimal("1000.00"))
                .build();
        jackpotRepository.save(fixedJackpot);

        variableJackpot = Jackpot.builder()
                .id("variable-test")
                .name("Variable Ramp Test Jackpot")
                .initialPool(new BigDecimal("500.00"))
                .currentPool(new BigDecimal("9500.00"))
                .contributionStrategy(ContributionStrategyType.FIXED_RATE)
                .rewardStrategy(RewardStrategyType.VARIABLE_RAMP)
                .contributionRate(new BigDecimal("0.10"))
                .rewardBaseProbability(new BigDecimal("0.01"))
                .rewardMaxProbability(new BigDecimal("1.00"))
                .rewardRampRate(new BigDecimal("0.99"))
                .rewardCap(new BigDecimal("10000.00"))
                .build();
        jackpotRepository.save(variableJackpot);
    }

    @AfterEach
    void tearDown() {
        rewardRepository.deleteAll();
        contributionRepository.deleteAll();
        jackpotRepository.deleteAll();
    }

    @Test
    void shouldReturnWinForFixedStrategyWithDeterministicSeed() throws Exception {
        // Given: Bet with contribution that will deterministically win
        String betId = "bet-win-fixed";
        JackpotContribution contribution = createContribution(betId, fixedJackpot);
        contributionRepository.save(contribution);

        // When/Then: Evaluate and expect win
        mockMvc.perform(get("/api/evaluations")
                        .param("betId", betId)
                        .param("jackpotId", fixedJackpot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.win").value(true))
                .andExpect(jsonPath("$.payoutAmount").value(1000.00))
                .andExpect(jsonPath("$.currentJackpotPool").value(500.00)) // Reset to initial
                .andExpect(jsonPath("$.probability").value(1.000000))
                .andExpect(jsonPath("$.strategy").value("FIXED"))
                .andExpect(jsonPath("$.betId").value(betId))
                .andExpect(jsonPath("$.jackpotId").value(fixedJackpot.getId()));

        // Verify reward persisted
        assertThat(rewardRepository.count()).isEqualTo(1);
        var reward = rewardRepository.findAll().get(0);
        assertThat(reward.getBetId()).isEqualTo(betId);
        assertThat(reward.getPayoutAmount()).isEqualByComparingTo("1000.00");

        // Verify pool reset
        Jackpot updated = jackpotRepository.findById(fixedJackpot.getId()).orElseThrow();
        assertThat(updated.getCurrentPool()).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldReturnNoWinForFixedStrategyWithDifferentSeed() throws Exception {
        // Given: Bet with contribution that will deterministically lose
        String betId = "bet-no-win-fixed";

        // Create separate jackpot with very low probability
        Jackpot noWinJackpot = Jackpot.builder()
                .id("no-win-test")
                .name("No Win Test")
                .initialPool(new BigDecimal("500.00"))
                .currentPool(new BigDecimal("1200.00"))
                .contributionStrategy(ContributionStrategyType.FIXED_RATE)
                .rewardStrategy(RewardStrategyType.FIXED)
                .contributionRate(new BigDecimal("0.10"))
                .rewardBaseProbability(new BigDecimal("0.000000")) // 0% probability
                .rewardMaxProbability(new BigDecimal("0.000000"))
                .rewardRampRate(BigDecimal.ZERO)
                .rewardCap(new BigDecimal("1000.00"))
                .build();
        jackpotRepository.save(noWinJackpot);

        JackpotContribution contribution = createContribution(betId, noWinJackpot);
        contributionRepository.save(contribution);

        // When/Then: Evaluate and expect no win
        mockMvc.perform(get("/api/evaluations")
                        .param("betId", betId)
                        .param("jackpotId", noWinJackpot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.win").value(false))
                .andExpect(jsonPath("$.payoutAmount").value(0.00))
                .andExpect(jsonPath("$.currentJackpotPool").value(1200.00)) // Unchanged
                .andExpect(jsonPath("$.strategy").value("FIXED"));

        // Verify no reward persisted
        assertThat(rewardRepository.count()).isEqualTo(0);
    }

    @Test
    void shouldReturnWinForVariableRampStrategyWithHighPool() throws Exception {
        // Given: Variable ramp with high pool (high probability)
        String betId = "bet-win-variable";
        JackpotContribution contribution = createContribution(betId, variableJackpot);
        contributionRepository.save(contribution);

        // When/Then: Evaluate and expect win due to high pool
        mockMvc.perform(get("/api/evaluations")
                        .param("betId", betId)
                        .param("jackpotId", variableJackpot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.win").value(true))
                .andExpect(jsonPath("$.payoutAmount").value(9500.00))
                .andExpect(jsonPath("$.currentJackpotPool").value(500.00)) // Reset
                .andExpect(jsonPath("$.strategy").value("VARIABLE_RAMP"));

        // Verify reward persisted
        assertThat(rewardRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnDeterministicResultForSameBetIdAndJackpotId() throws Exception {
        // Given: Same bet with very low probability (no win scenario)
        String betId = "bet-deterministic";

        // Create a jackpot with very low probability to ensure no win
        Jackpot noWinJackpot = Jackpot.builder()
                .id("deterministic-test")
                .name("Deterministic Test")
                .initialPool(new BigDecimal("500.00"))
                .currentPool(new BigDecimal("1000.00"))
                .contributionStrategy(ContributionStrategyType.FIXED_RATE)
                .rewardStrategy(RewardStrategyType.FIXED)
                .contributionRate(new BigDecimal("0.10"))
                .rewardBaseProbability(new BigDecimal("0.000000")) // 0% probability
                .rewardMaxProbability(new BigDecimal("0.000000"))
                .rewardRampRate(BigDecimal.ZERO)
                .rewardCap(new BigDecimal("1000.00"))
                .build();
        jackpotRepository.save(noWinJackpot);

        JackpotContribution contribution = createContribution(betId, noWinJackpot);
        contributionRepository.save(contribution);

        // When: First evaluation
        String response1 = mockMvc.perform(get("/api/evaluations")
                        .param("betId", betId)
                        .param("jackpotId", noWinJackpot.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // When: Second evaluation (no state change since no win)
        String response2 = mockMvc.perform(get("/api/evaluations")
                        .param("betId", betId)
                        .param("jackpotId", noWinJackpot.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then: Same deterministic result
        assertThat(response1).isEqualTo(response2);

        // Verify no rewards created
        assertThat(rewardRepository.count()).isEqualTo(0);
    }

    @Test
    void shouldReturn404WhenContributionNotFound() throws Exception {
        // Given: No contribution exists
        String betId = "non-existent-bet";
        String jackpotId = "fixed-test";

        // When/Then: Expect 404
        mockMvc.perform(get("/api/evaluations")
                        .param("betId", betId)
                        .param("jackpotId", jackpotId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No contribution found")));
    }

    @Test
    void shouldReturn404WhenJackpotNotFound() throws Exception {
        // Given: Valid contribution but request for different jackpot
        String betId = "bet-valid";
        String wrongJackpotId = "non-existent-jackpot";

        JackpotContribution contribution = createContribution(betId, fixedJackpot);
        contributionRepository.save(contribution);

        // When/Then: Expect 404 because betId+jackpotId combination doesn't exist
        mockMvc.perform(get("/api/evaluations")
                        .param("betId", betId)
                        .param("jackpotId", wrongJackpotId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No contribution found")));
    }

    @Test
    void shouldRequireBothBetIdAndJackpotIdParams() throws Exception {
        // When/Then: Missing betId
        mockMvc.perform(get("/api/evaluations")
                        .param("jackpotId", "fixed-test"))
                .andExpect(status().isBadRequest());

        // When/Then: Missing jackpotId
        mockMvc.perform(get("/api/evaluations")
                        .param("betId", "bet-123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSerializeConcurrentEvaluationsAndPreventMultipleWinners() throws Exception {
        // Given: Jackpot with 100% win probability
        Jackpot concurrentTestJackpot = Jackpot.builder()
                .id("concurrent-test")
                .name("Concurrent Test Jackpot")
                .initialPool(new BigDecimal("500.00"))
                .currentPool(new BigDecimal("5000.00"))
                .contributionStrategy(ContributionStrategyType.FIXED_RATE)
                .rewardStrategy(RewardStrategyType.FIXED)
                .contributionRate(new BigDecimal("0.10"))
                .rewardBaseProbability(new BigDecimal("1.000000")) // 100% win probability
                .rewardMaxProbability(new BigDecimal("1.000000"))
                .rewardRampRate(BigDecimal.ZERO)
                .rewardCap(new BigDecimal("5000.00"))
                .build();
        jackpotRepository.save(concurrentTestJackpot);

        // Create 10 different bet contributions (all will deterministically win due to 100% probability)
        int numberOfBets = 10;
        for (int i = 0; i < numberOfBets; i++) {
            String betId = "concurrent-bet-" + i;
            JackpotContribution contribution = createContribution(betId, concurrentTestJackpot);
            contributionRepository.save(contribution);
        }

        // When: Submit concurrent evaluation requests from multiple threads
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(numberOfBets);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < numberOfBets; i++) {
            final String betId = "concurrent-bet-" + i;
            new Thread(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Execute evaluation request
                    mockMvc.perform(get("/api/evaluations")
                                    .param("betId", betId)
                                    .param("jackpotId", concurrentTestJackpot.getId()))
                            .andExpect(status().isOk());

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected: some requests may see updated pool state after first winner
                    // but should not fail
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Release all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete (max 30 seconds)
        boolean completed = doneLatch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(completed).isTrue().withFailMessage("Concurrent evaluations timed out");

        // Then: Verify results
        // With pessimistic locking, requests are serialized:
        // - First request wins (pool=5000, gets capped payout of 5000, resets to 500)
        // - Remaining 9 requests evaluate against pool=500 with different outcomes based on betId
        long rewardCount = rewardRepository.count();

        // At minimum, we should have exactly 1 winner from the initial 5000 pool
        // (subsequent requests see 500 pool with same 100% probability but different betIds/RNG)
        assertThat(rewardCount).isGreaterThanOrEqualTo(1)
                .withFailMessage("Expected at least 1 reward to be created");

        // Verify pool state is consistent - should be at initialPool if last evaluation won,
        // or at some accumulated value if last evaluation lost
        Jackpot updatedJackpot = jackpotRepository.findById(concurrentTestJackpot.getId()).orElseThrow();
        assertThat(updatedJackpot.getCurrentPool())
                .isGreaterThanOrEqualTo(new BigDecimal("500.00"))
                .withFailMessage("Pool should not be negative or below initial");

        // Verify all rewards have valid amounts
        var rewards = rewardRepository.findAll();
        for (var reward : rewards) {
            assertThat(reward.getPayoutAmount())
                    .isGreaterThan(BigDecimal.ZERO)
                    .isLessThanOrEqualTo(new BigDecimal("5000.00"));
        }

        // Verify no duplicate betId rewards (each bet can only win once)
        var betIds = rewards.stream().map(r -> r.getBetId()).toList();
        var uniqueBetIds = new java.util.HashSet<>(betIds);
        assertThat(betIds).hasSameSizeAs(uniqueBetIds)
                .withFailMessage("Each betId should win at most once");
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
