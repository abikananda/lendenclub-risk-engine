package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.dto.BorrowerLookupResponse;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.exception.ResourceNotFoundException;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowerLookupService {

    private final BorrowerSnapshotRepository borrowerSnapshotRepository;

    public BorrowerLookupService(BorrowerSnapshotRepository borrowerSnapshotRepository) {
        this.borrowerSnapshotRepository = borrowerSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public BorrowerLookupResponse findByLoanId(String loanId) {
        String normalizedLoanId = loanId == null ? "" : loanId.trim();
        if (normalizedLoanId.isEmpty()) {
            throw new ResourceNotFoundException("Borrower not found for loan ID " + loanId);
        }

        BorrowerSnapshot snapshot = borrowerSnapshotRepository
                .findTopByLoanIdAndBorrowerNameIsNotNullOrderByScrapedAtDesc(normalizedLoanId)
                .filter(value -> !value.getBorrowerName().isBlank())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Borrower not found for loan ID " + normalizedLoanId));

        return new BorrowerLookupResponse(snapshot.getLoanId(), snapshot.getBorrowerName().trim());
    }
}
