package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.domain.LendingSessionStatus;
import com.abikananda.lendenclub.entity.LendingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LendingSessionRepository extends JpaRepository<LendingSession, Long> {
    Optional<LendingSession> findBySessionId(String sessionId);

    Optional<LendingSession> findBySessionIdAndStatus(String sessionId, LendingSessionStatus status);
}
