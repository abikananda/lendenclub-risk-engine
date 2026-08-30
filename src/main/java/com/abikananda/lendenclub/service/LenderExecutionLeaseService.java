package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.entity.Lender;
import com.abikananda.lendenclub.exception.LenderExecutionLockedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class LenderExecutionLeaseService {

    private final JdbcTemplate jdbcTemplate;
    private final Duration leaseDuration;

    public LenderExecutionLeaseService(
            JdbcTemplate jdbcTemplate,
            @Value("${lending.execution-lease-minutes:45}") long leaseMinutes) {
        this.jdbcTemplate = jdbcTemplate;
        this.leaseDuration = Duration.ofMinutes(leaseMinutes);
    }

    @Transactional
    public void acquire(Lender lender, String sessionId, String ownerId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(leaseDuration);

        jdbcTemplate.update("""
                INSERT INTO lender_execution_lock
                    (lender_id, session_id, owner_id, acquired_at, heartbeat_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    session_id = IF(expires_at < VALUES(acquired_at), VALUES(session_id), session_id),
                    owner_id = IF(expires_at < VALUES(acquired_at), VALUES(owner_id), owner_id),
                    acquired_at = IF(expires_at < VALUES(acquired_at), VALUES(acquired_at), acquired_at),
                    heartbeat_at = IF(expires_at < VALUES(acquired_at), VALUES(heartbeat_at), heartbeat_at),
                    expires_at = IF(expires_at < VALUES(acquired_at), VALUES(expires_at), expires_at)
                """,
                lender.getId(), sessionId, ownerId,
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(expiresAt));

        Map<String, Object> lock = jdbcTemplate.queryForMap(
                "SELECT session_id, owner_id, expires_at FROM lender_execution_lock WHERE lender_id = ?",
                lender.getId());

        if (!sessionId.equals(lock.get("session_id"))) {
            throw new LenderExecutionLockedException(
                    "Lender is already running in another workflow. username=" + lender.getUsername()
                            + " activeSession=" + lock.get("session_id")
                            + " owner=" + lock.get("owner_id")
                            + " expiresAt=" + lock.get("expires_at"));
        }
    }

    @Transactional
    public void heartbeat(String sessionId) {
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                "UPDATE lender_execution_lock SET heartbeat_at = ?, expires_at = ? WHERE session_id = ?",
                Timestamp.from(now), Timestamp.from(now.plus(leaseDuration)), sessionId);
        if (updated != 1) {
            throw new LenderExecutionLockedException("No active execution lease for session: " + sessionId);
        }
    }

    @Transactional
    public void release(String sessionId) {
        jdbcTemplate.update("DELETE FROM lender_execution_lock WHERE session_id = ?", sessionId);
    }
}
