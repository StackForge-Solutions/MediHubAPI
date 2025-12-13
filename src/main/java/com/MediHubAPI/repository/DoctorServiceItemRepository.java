package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.DoctorServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DoctorServiceItemRepository
        extends JpaRepository<DoctorServiceItem, Long>, JpaSpecificationExecutor<DoctorServiceItem> {

    boolean existsByDoctor_IdAndNameIgnoreCase(Long doctorId, String name);
}
