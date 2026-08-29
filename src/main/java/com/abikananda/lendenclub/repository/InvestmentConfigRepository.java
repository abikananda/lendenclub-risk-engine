package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.InvestmentConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestmentConfigRepository extends JpaRepository<InvestmentConfig, Long> {
    Optional<InvestmentConfig> findByLender_IdAndEnabledTrue(Long lenderId);
}
