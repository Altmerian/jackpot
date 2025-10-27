package com.pshakhlovich.jackpot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pshakhlovich.jackpot.domain.model.JackpotContribution;
import java.util.UUID;

public interface JackpotContributionRepository extends JpaRepository<JackpotContribution, UUID> {
}
