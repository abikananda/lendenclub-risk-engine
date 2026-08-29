package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.dto.LenderResponse;
import com.abikananda.lendenclub.dto.LendingSessionResponse;
import com.abikananda.lendenclub.service.LenderService;
import com.abikananda.lendenclub.service.LendingSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lender")
public class LenderController {

    private final LenderService lenderService;
    private final LendingSessionService sessionService;

    public LenderController(LenderService lenderService, LendingSessionService sessionService) {
        this.lenderService = lenderService;
        this.sessionService = sessionService;
    }

    @GetMapping("/data")
    public ResponseEntity<LenderResponse> getLenderData(
            @RequestParam(name = "username", required = false) String username) {
        return ResponseEntity.ok(lenderService.getLenderAndStartSession(username));
    }

    @PostMapping("/session/{sessionId}/complete")
    public ResponseEntity<LendingSessionResponse> completeSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.completeSession(sessionId));
    }

    @PostMapping("/session/{sessionId}/cancel")
    public ResponseEntity<LendingSessionResponse> cancelSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.cancelSession(sessionId));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<LendingSessionResponse> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getSessionSummary(sessionId));
    }
}
