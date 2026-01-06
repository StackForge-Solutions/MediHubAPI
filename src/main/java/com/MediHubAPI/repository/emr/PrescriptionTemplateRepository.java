package com.MediHubAPI.repository.emr;

import com.MediHubAPI.model.emr.PrescriptionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionTemplateRepository extends JpaRepository<PrescriptionTemplate, Long> {
    Optional<PrescriptionTemplate> findByNameIgnoreCase(String name);
}
