package com.pshakhlovich.jackpot.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.pshakhlovich.jackpot.domain.model.JackpotReward;

public interface JackpotRewardRepository extends JpaRepository<JackpotReward, UUID> {
}
