package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.PharmacyStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PharmacyStockRepository extends JpaRepository<PharmacyStock, Long> {
    Optional<PharmacyStock> findByMedicine_Id(Long medicineId);
}
