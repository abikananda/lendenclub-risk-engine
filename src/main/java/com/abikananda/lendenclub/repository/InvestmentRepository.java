package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.Investment;
import com.abikananda.lendenclub.domain.InvestmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    Optional<Investment> findByExternalInvestmentId(String externalInvestmentId);
    List<Investment> findByLoanId(String loanId);
    Page<Investment> findAllByOrderByRequestedAtDesc(Pageable pageable);
    
    @Query("SELECT COUNT(i) FROM Investment i WHERE i.sessionId = :sessionId")
    long countBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT COUNT(i) FROM Investment i WHERE i.status = :status")
    long countByStatus(@Param("status") InvestmentStatus status);

    @Query("SELECT COALESCE(SUM(i.requestedAmount), 0) FROM Investment i WHERE i.status = 'SUCCESS'")
    BigDecimal sumTotalSuccessfulAmount();

    @Query("SELECT COUNT(i) FROM Investment i WHERE i.status = 'SUCCESS' AND i.requestedAt >= :startOfDay")
    long countTodaySuccessful(@Param("startOfDay") OffsetDateTime startOfDay);

    @Query("SELECT COALESCE(SUM(i.requestedAmount), 0) FROM Investment i WHERE i.status = 'SUCCESS' AND i.requestedAt >= :startOfDay")
    BigDecimal sumTodaySuccessfulAmount(@Param("startOfDay") OffsetDateTime startOfDay);
}
