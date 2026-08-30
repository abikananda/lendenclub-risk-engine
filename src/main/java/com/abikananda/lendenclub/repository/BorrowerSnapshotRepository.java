package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BorrowerSnapshotRepository extends JpaRepository<BorrowerSnapshot, Long> {
    Optional<BorrowerSnapshot> findTopBySessionIdAndLoanIdOrderByScrapedAtDesc(String sessionId, String loanId);
}
