package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.TrustedBorrower;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrustedBorrowerRepository extends JpaRepository<TrustedBorrower, Long> {
    boolean existsByBorrowerProfile_IdAndActiveTrue(Long borrowerProfileId);
    Optional<TrustedBorrower> findByBorrowerProfile_Id(Long borrowerProfileId);
}
