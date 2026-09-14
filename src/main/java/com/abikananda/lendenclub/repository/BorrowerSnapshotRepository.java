package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface BorrowerSnapshotRepository extends JpaRepository<BorrowerSnapshot, Long> {
    List<BorrowerSnapshot> findByLoanId(String loanId);
    Optional<BorrowerSnapshot> findTopBySessionIdAndLoanIdOrderByScrapedAtDesc(String sessionId, String loanId);

    Optional<BorrowerSnapshot> findTopByLoanIdAndBorrowerNameIsNotNullOrderByScrapedAtDesc(String loanId);
}
