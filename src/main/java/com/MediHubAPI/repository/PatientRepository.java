package com.MediHubAPI.repository;

import com.MediHubAPI.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByHospitalId(String hospitalId);
    Optional<Patient> findByHospitalId(String hospitalId);
}