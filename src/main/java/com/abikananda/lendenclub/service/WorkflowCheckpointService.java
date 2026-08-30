package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.WorkflowCheckpointRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class WorkflowCheckpointService {

    private static final Set<String> ALLOWED_STATES = Set.of(
            "DISCOVERED",
            "EXTRACTED",
            "EVALUATED",
            "APPROVED",
            "UI_SELECTED",
            "CONTINUE_CLICKED",
            "PLATFORM_CONFIRMED",
            "BACKEND_RECORDED",
            "SKIPPED",
            "FAILED",
            "UNCERTAIN"
    );

    private final JdbcTemplate jdbcTemplate;
    private final LendingSessionService sessionService;

    public WorkflowCheckpointService(JdbcTemplate jdbcTemplate, LendingSessionService sessionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sessionService = sessionService;
    }

    @Transactional
    public void record(String sessionId, WorkflowCheckpointRequest request) {
        sessionService.requireActiveSession(sessionId);
        String state = request.getState().trim().toUpperCase();
        if (!ALLOWED_STATES.contains(state)) {
            throw new IllegalArgumentException("Unsupported workflow checkpoint state: " + request.getState());
        }

        jdbcTemplate.update("""
                INSERT INTO workflow_checkpoint (session_id, loan_id, rule_name, state, message)
                VALUES (?, ?, ?, ?, ?)
                """,
                sessionId,
                blankToNull(request.getLoanId()),
                blankToNull(request.getRule()),
                state,
                blankToNull(request.getMessage()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
