package com.MediHubAPI.repository.emr;

import com.MediHubAPI.model.emr.PrescriptionMedication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionMedicationRepository extends JpaRepository<PrescriptionMedication, Long> {
    List<PrescriptionMedication> findByPrescription_Id(Long prescriptionId);
    void deleteByPrescription_Id(Long prescriptionId);
}
