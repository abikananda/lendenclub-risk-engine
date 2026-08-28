package com.abikananda.lendenclub.repository;

import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerSnapshotRepository extends JpaRepository<BorrowerSnapshot, Long> {
}
