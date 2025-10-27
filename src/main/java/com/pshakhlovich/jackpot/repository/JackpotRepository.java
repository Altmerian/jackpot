package com.pshakhlovich.jackpot.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import com.pshakhlovich.jackpot.domain.model.Jackpot;

public interface JackpotRepository extends JpaRepository<Jackpot, String> {

    Optional<Jackpot> findByName(String name);

    /**
     * Acquires a pessimistic write lock on the jackpot row to ensure exclusive access
     * for evaluation and contribution operations. This prevents race conditions where
     * multiple concurrent requests could read the same pool state and create multiple
     * winners or inconsistent contributions.
     *
     * <p>Lock timeout is set to 5000ms (5 seconds). If the lock cannot be acquired
     * within this time, a PessimisticLockException will be thrown.</p>
     *
     * @param id the jackpot identifier
     * @return Optional containing the locked jackpot if found
     * @throws jakarta.persistence.PessimisticLockException if lock cannot be acquired within timeout
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT j FROM Jackpot j WHERE j.id = :id")
    Optional<Jackpot> findByIdForUpdate(@Param("id") String id);
}
