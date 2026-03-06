package com.MediHubAPI.repository.emr;

import com.MediHubAPI.model.emr.IpAdmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpAdmissionRepository extends JpaRepository<IpAdmission, Long> {
    Optional<IpAdmission> findByAppointment_Id(Long appointmentId);
}
