package com.pshakhlovich.jackpot.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pshakhlovich.jackpot.avro.Bet;
import com.pshakhlovich.jackpot.config.KafkaTopicsConfig;
import com.pshakhlovich.jackpot.domain.model.ContributionStrategyType;
import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.domain.model.RewardStrategyType;
import com.pshakhlovich.jackpot.repository.JackpotContributionRepository;
import com.pshakhlovich.jackpot.repository.JackpotRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.awaitility.Awaitility;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;

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
class BetControllerIntegrationTests {

    private static final String SCHEMA_REGISTRY_MOCK_URL = "mock://jackpot";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private JackpotContributionRepository contributionRepository;

    @Autowired
    private JackpotRepository jackpotRepository;

    private Consumer<String, Bet> avroConsumer;

    @BeforeEach
    void setUpConsumer() {
        // Seed jackpot profiles for the test
        seedJackpotProfiles();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("bet-controller-tests", "false", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        consumerProps.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_MOCK_URL);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        DefaultKafkaConsumerFactory<String, Bet> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        avroConsumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(avroConsumer, KafkaTopicsConfig.BETS_TOPIC);
    }

    private void seedJackpotProfiles() {
        if (jackpotRepository.findById("fixed-warmup").isEmpty()) {
            Jackpot fixedWarmup = Jackpot.builder()
                    .id("fixed-warmup")
                    .name("Fixed Warmup")
                    .initialPool(new BigDecimal("500.00"))
                    .currentPool(new BigDecimal("500.00"))
                    .contributionStrategy(ContributionStrategyType.FIXED_RATE)
                    .rewardStrategy(RewardStrategyType.FIXED)
                    .contributionRate(new BigDecimal("0.10"))
                    .rewardBaseProbability(new BigDecimal("0.05"))
                    .rewardMaxProbability(new BigDecimal("0.05"))
                    .rewardRampRate(BigDecimal.ZERO)
                    .rewardCap(new BigDecimal("1000.00"))
                    .build();
            jackpotRepository.save(fixedWarmup);
        }
    }

    @AfterEach
    void tearDown() {
        if (avroConsumer != null) {
            avroConsumer.close();
        }
        contributionRepository.deleteAll();
        jackpotRepository.findAll().forEach(jackpot -> {
            jackpot.setCurrentPool(jackpot.getInitialPool());
            jackpotRepository.save(jackpot);
        });
    }

    @Test
    void shouldPublishBetAndPersistContribution() throws Exception {
        String payload = "{" +
                "\"betId\":\"bet-integration\"," +
                "\"userId\":\"user-1\"," +
                "\"jackpotId\":\"fixed-warmup\"," +
                "\"betAmount\":50.0" +
                "}";

        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        var records = KafkaTestUtils.getRecords(avroConsumer, Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);
        Bet bet = records.iterator().next().value();
        assertThat(bet.getBetId()).isEqualTo("bet-integration");
        assertThat(bet.getJackpotId()).isEqualTo("fixed-warmup");
        assertThat(bet.getBetAmount()).isEqualTo(50.0);

        KafkaTestUtils.getRecords(avroConsumer, Duration.ofSeconds(1));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(contributionRepository.count()).isEqualTo(1));

        var contribution = contributionRepository.findAll().get(0);
        assertThat(contribution.getContributionAmount()).isEqualByComparingTo("5.00");
        assertThat(contribution.getPostContributionPool()).isEqualByComparingTo("505.00");
    }
}
