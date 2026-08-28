package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.dto.InvestmentResponse;
import com.abikananda.lendenclub.dto.InvestmentStatusRequest;
import com.abikananda.lendenclub.dto.InvestmentSummaryResponse;
import com.abikananda.lendenclub.entity.Investment;
import com.abikananda.lendenclub.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/investment")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @GetMapping("/data")
    public ResponseEntity<InvestmentSummaryResponse> getInvestmentData() {
        return ResponseEntity.ok(investmentService.getSummaryData());
    }

    @PostMapping("/status")
    public ResponseEntity<InvestmentResponse> recordStatus(@Valid @RequestBody InvestmentStatusRequest request) {
        return ResponseEntity.ok(investmentService.recordStatus(request));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<List<Investment>> getInvestmentsForLoan(@PathVariable String loanId) {
        return ResponseEntity.ok(investmentService.getInvestmentsForLoan(loanId));
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<Investment>> getRecentInvestments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(investmentService.getRecentInvestments(PageRequest.of(page, size)));
    }
}
