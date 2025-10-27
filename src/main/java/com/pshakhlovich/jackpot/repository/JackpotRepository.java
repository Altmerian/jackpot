package com.pshakhlovich.jackpot.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.pshakhlovich.jackpot.domain.model.Jackpot;

public interface JackpotRepository extends JpaRepository<Jackpot, String> {

    Optional<Jackpot> findByName(String name);
}
