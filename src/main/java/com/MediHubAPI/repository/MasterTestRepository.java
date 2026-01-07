package com.MediHubAPI.repository;

import com.MediHubAPI.model.mdm.PathologyTestMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface MasterTestRepository extends JpaRepository<PathologyTestMaster, Long> {

    Optional<PathologyTestMaster> findByNameIgnoreCase(String name);

    List<PathologyTestMaster> findByCategoryIgnoreCase(String category);

    List<PathologyTestMaster> findByNameContainingIgnoreCase(String keyword);

    // Only active tests
    List<PathologyTestMaster> findByIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String q, Pageable pageable);
}
