package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.domain.LendingRule;
import com.abikananda.lendenclub.dto.BorrowerEvaluateRequest;
import com.abikananda.lendenclub.dto.BorrowerEvaluateResponse;
import com.abikananda.lendenclub.entity.BorrowerEvaluation;
import com.abikananda.lendenclub.service.BorrowerEvaluationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/borrower")
public class BorrowerController {

    private final BorrowerEvaluationService evaluationService;

    public BorrowerController(BorrowerEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<BorrowerEvaluateResponse> evaluateBorrower(@Valid @RequestBody BorrowerEvaluateRequest request) {
        return ResponseEntity.ok(evaluationService.evaluateAndSave(request));
    }
    @PostMapping("/evaluate/{ruleCode}")
    public ResponseEntity<BorrowerEvaluateResponse> evaluateBorrower(@Valid @RequestBody BorrowerEvaluateRequest request, @PathVariable String ruleCode) {
        return ResponseEntity.ok(evaluationService.evaluateSpecificRule(request, LendingRule.fromCode(ruleCode)));
    }

    @GetMapping("/{loanId}/evaluations")
    public ResponseEntity<List<BorrowerEvaluation>> getEvaluationsForLoan(@PathVariable String loanId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsForLoan(loanId));
    }

    @GetMapping("/evaluations/recent")
    public ResponseEntity<Page<BorrowerEvaluation>> getRecentEvaluations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(evaluationService.getRecentEvaluations(PageRequest.of(page, size)));
    }
}
