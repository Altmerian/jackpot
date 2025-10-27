package com.pshakhlovich.jackpot.config;

import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.repository.JackpotRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JackpotProperties jackpotProperties;
    private final JackpotRepository jackpotRepository;

    @Override
    public void run(String... args) {
        List<JackpotProperties.JackpotProfileProperties> profiles = jackpotProperties.profiles();
        if (profiles.isEmpty()) {
            log.warn("No jackpot profiles configured; skipping seeding");
            return;
        }

        profiles.forEach(profile -> jackpotRepository.findById(profile.id())
                .ifPresentOrElse(
                        existing -> log.debug("Jackpot profile {} already present", existing.getId()),
                        () -> {
                            Jackpot jackpot = Jackpot.builder()
                                    .id(profile.id())
                                    .name(profile.name())
                                    .initialPool(profile.initialPool())
                                    .currentPool(profile.initialPool())
                                    .contributionStrategy(profile.contributionStrategy())
                                    .rewardStrategy(profile.rewardStrategy())
                                    .contributionRate(profile.contribution() != null ? profile.contribution().rate() : null)
                                    .minContributionRate(profile.contribution() != null ? profile.contribution().minRate() : null)
                                    .decayThreshold(profile.contribution() != null ? profile.contribution().decayThreshold() : null)
                                    .decaySlope(profile.contribution() != null ? profile.contribution().decaySlope() : null)
                                    .rewardBaseProbability(profile.reward() != null ? profile.reward().baseProbability() : null)
                                    .rewardMaxProbability(profile.reward() != null ? profile.reward().maxProbability() : null)
                                    .rewardRampRate(profile.reward() != null ? profile.reward().rampRate() : null)
                                    .rewardCap(profile.reward() != null ? profile.reward().cap() : null)
                                    .build();

                            jackpotRepository.save(jackpot);
                            log.info("Seeded jackpot profile {}", profile.id());
                        }));
    }
}
