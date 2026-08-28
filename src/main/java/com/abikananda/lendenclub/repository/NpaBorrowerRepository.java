package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.NpaBorrower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface NpaBorrowerRepository extends JpaRepository<NpaBorrower, Long> {

    List<NpaBorrower> findAllByActiveTrueOrderByBorrowerNameAsc();

    @Modifying
    @Query("""
            update NpaBorrower n
               set n.hitCount = n.hitCount + 1,
                   n.lastHitAt = :hitAt,
                   n.lastSessionId = :sessionId,
                   n.lastLoanId = :loanId
             where n.id = :id and n.active = true
            """)
    int recordHit(@Param("id") Long id,
                  @Param("hitAt") OffsetDateTime hitAt,
                  @Param("sessionId") String sessionId,
                  @Param("loanId") String loanId);
}
