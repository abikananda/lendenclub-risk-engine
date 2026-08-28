package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.domain.LendingSessionStatus;
import com.abikananda.lendenclub.entity.LendingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LendingSessionRepository extends JpaRepository<LendingSession, Long> {
    Optional<LendingSession> findBySessionId(String sessionId);

    Optional<LendingSession> findBySessionIdAndStatus(String sessionId, LendingSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LendingSession s join fetch s.lender where s.sessionId = :sessionId")
    Optional<LendingSession> findBySessionIdForUpdate(@Param("sessionId") String sessionId);
}
