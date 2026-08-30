package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BorrowerProfileRepository extends JpaRepository<BorrowerProfile, Long> {
    List<BorrowerProfile> findByNormalizedName(String normalizedName);
    Optional<BorrowerProfile> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BorrowerProfile p where p.id = :id")
    Optional<BorrowerProfile> findByIdForUpdate(@Param("id") Long id);
}
