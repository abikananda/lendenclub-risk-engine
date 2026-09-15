package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.domain.AiRiskResult;
import com.abikananda.lendenclub.dto.AiRiskTestRequest;
import com.abikananda.lendenclub.service.AiRiskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-risk")
public class AiRiskTestController {

    private final AiRiskService aiRiskService;

    public AiRiskTestController(AiRiskService aiRiskService) {
        this.aiRiskService = aiRiskService;
    }

    /**
     * Runs only the configured AI reviewer. It does not execute Drools,
     * persist an evaluation, or initiate an investment.
     */
    @PostMapping("/test")
    public ResponseEntity<AiRiskResult> test(@Valid @RequestBody AiRiskTestRequest request) {
        return ResponseEntity.ok(aiRiskService.evaluate(request.toFact()));
    }
}
