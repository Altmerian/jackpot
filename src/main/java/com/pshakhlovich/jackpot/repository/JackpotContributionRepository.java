package com.pshakhlovich.jackpot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.pshakhlovich.jackpot.domain.model.JackpotContribution;
import java.util.Optional;
import java.util.UUID;

public interface JackpotContributionRepository extends JpaRepository<JackpotContribution, UUID> {

    @Query("SELECT c FROM JackpotContribution c WHERE c.betId = :betId AND c.jackpot.id = :jackpotId")
    Optional<JackpotContribution> findByBetIdAndJackpotId(@Param("betId") String betId, @Param("jackpotId") String jackpotId);
}
