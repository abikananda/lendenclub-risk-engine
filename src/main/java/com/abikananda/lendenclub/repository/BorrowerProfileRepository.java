package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BorrowerProfileRepository extends JpaRepository<BorrowerProfile, Long> {
    List<BorrowerProfile> findByNormalizedName(String normalizedName);
    Optional<BorrowerProfile> findByPublicId(String publicId);
}
