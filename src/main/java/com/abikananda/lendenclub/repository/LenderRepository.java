package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.Lender;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LenderRepository extends JpaRepository<Lender, Long> {
    Optional<Lender> findByExternalLenderId(String externalLenderId);
    Optional<Lender> findFirstByActiveTrue();
}
