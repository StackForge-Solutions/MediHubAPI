package com.MediHubAPI.repository.emr;

import com.MediHubAPI.model.emr.PrescriptionTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionTemplateRepository extends JpaRepository<PrescriptionTemplate, Long> {

    Optional<PrescriptionTemplate> findByNameIgnoreCase(String name);

    // for update: exclude self id
    Optional<PrescriptionTemplate> findByNameIgnoreCaseAndIdNot(String name, Long id);

    // list with query
    List<PrescriptionTemplate> findByIsActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(String query, Pageable pageable);

    // list without query
    List<PrescriptionTemplate> findByIsActiveTrueOrderByUpdatedAtDesc(Pageable pageable);

    boolean existsByNameIgnoreCase(String name);


}
