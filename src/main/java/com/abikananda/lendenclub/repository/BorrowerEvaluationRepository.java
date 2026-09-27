package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.domain.LendingDecision;
import com.abikananda.lendenclub.entity.BorrowerEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BorrowerEvaluationRepository extends JpaRepository<BorrowerEvaluation, Long> {
    List<BorrowerEvaluation> findByLoanId(String loanId);
    Optional<BorrowerEvaluation> findFirstBySessionIdAndLoanIdAndDecisionOrderByEvaluatedAtDesc(
            String sessionId, String loanId, LendingDecision decision);
    Page<BorrowerEvaluation> findAllByOrderByEvaluatedAtDesc(Pageable pageable);
}
