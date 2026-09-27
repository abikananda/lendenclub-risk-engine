package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.domain.LendingDecision;
import com.abikananda.lendenclub.entity.BorrowerEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BorrowerEvaluationRepository extends JpaRepository<BorrowerEvaluation, Long> {
    @Modifying
    @Query(value = "INSERT IGNORE INTO loan_approval (lender_id, loan_id, session_id) VALUES (:lenderId, :loanId, :sessionId)", nativeQuery = true)
    int claimLoan(@Param("lenderId") Long lenderId, @Param("loanId") String loanId,
                  @Param("sessionId") String sessionId);

    @Modifying
    @Query(value = "UPDATE loan_approval SET invested_at = CURRENT_TIMESTAMP WHERE lender_id = :lenderId AND loan_id = :loanId AND session_id = :sessionId AND invested_at IS NULL", nativeQuery = true)
    int markInvested(@Param("lenderId") Long lenderId, @Param("loanId") String loanId,
                     @Param("sessionId") String sessionId);

    List<BorrowerEvaluation> findByLoanId(String loanId);
    Optional<BorrowerEvaluation> findFirstBySessionIdAndLoanIdAndDecisionOrderByEvaluatedAtDesc(
            String sessionId, String loanId, LendingDecision decision);
    Page<BorrowerEvaluation> findAllByOrderByEvaluatedAtDesc(Pageable pageable);
}
