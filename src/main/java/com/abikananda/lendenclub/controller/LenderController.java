package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.dto.LenderDto;
import com.abikananda.lendenclub.dto.LenderResponse;
import com.abikananda.lendenclub.dto.LendingSessionResponse;
import com.abikananda.lendenclub.dto.StartLendingSessionRequest;
import com.abikananda.lendenclub.dto.WorkflowCheckpointRequest;
import com.abikananda.lendenclub.service.LenderExecutionLeaseService;
import com.abikananda.lendenclub.service.LenderService;
import com.abikananda.lendenclub.service.LendingSessionService;
import com.abikananda.lendenclub.service.WorkflowCheckpointService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lender")
public class LenderController {

    private final LenderService lenderService;
    private final LendingSessionService sessionService;
    private final LenderExecutionLeaseService leaseService;
    private final WorkflowCheckpointService checkpointService;

    public LenderController(LenderService lenderService,
                            LendingSessionService sessionService,
                            LenderExecutionLeaseService leaseService,
                            WorkflowCheckpointService checkpointService) {
        this.lenderService = lenderService;
        this.sessionService = sessionService;
        this.leaseService = leaseService;
        this.checkpointService = checkpointService;
    }

    @GetMapping("/config")
    public ResponseEntity<LenderDto> getLenderConfig(
            @RequestParam(name = "username", required = false) String username) {
        return ResponseEntity.ok(lenderService.getLenderConfig(username));
    }

    /**
     * Backward-compatible read-only alias. This endpoint no longer creates a session.
     */
    @Deprecated
    @GetMapping("/data")
    public ResponseEntity<LenderDto> getLenderData(
            @RequestParam(name = "username", required = false) String username) {
        return ResponseEntity.ok(lenderService.getLenderConfig(username));
    }

    @PostMapping("/session")
    public ResponseEntity<LenderResponse> startSession(
            @RequestParam(name = "username", required = false) String username,
            @Valid @RequestBody StartLendingSessionRequest request) {
        return ResponseEntity.ok(lenderService.startSession(username, request.getOwnerId()));
    }

    @PostMapping("/session/{sessionId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable String sessionId) {
        sessionService.requireActiveSession(sessionId);
        leaseService.heartbeat(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/session/{sessionId}/checkpoint")
    public ResponseEntity<Void> checkpoint(
            @PathVariable String sessionId,
            @Valid @RequestBody WorkflowCheckpointRequest request) {
        checkpointService.record(sessionId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/session/{sessionId}/complete")
    public ResponseEntity<LendingSessionResponse> completeSession(@PathVariable String sessionId) {
        LendingSessionResponse response = sessionService.completeSession(sessionId);
        leaseService.release(sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/session/{sessionId}/cancel")
    public ResponseEntity<LendingSessionResponse> cancelSession(@PathVariable String sessionId) {
        LendingSessionResponse response = sessionService.cancelSession(sessionId);
        leaseService.release(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<LendingSessionResponse> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getSessionSummary(sessionId));
    }
}
