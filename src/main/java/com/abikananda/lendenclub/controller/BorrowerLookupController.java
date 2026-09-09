package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.dto.BorrowerLookupResponse;
import com.abikananda.lendenclub.service.BorrowerLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrowers")
public class BorrowerLookupController {

    private final BorrowerLookupService borrowerLookupService;

    public BorrowerLookupController(BorrowerLookupService borrowerLookupService) {
        this.borrowerLookupService = borrowerLookupService;
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<BorrowerLookupResponse> getByLoanId(@PathVariable String loanId) {
        return ResponseEntity.ok(borrowerLookupService.findByLoanId(loanId));
    }
}
