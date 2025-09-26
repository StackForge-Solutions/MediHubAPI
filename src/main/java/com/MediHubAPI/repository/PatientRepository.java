package com.MediHubAPI.repository;


import com.MediHubAPI.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PatientRepository extends JpaRepository<User, Long> {

    // Filter by exact date
    List<User> findByActivationDate(LocalDate date);

    // Filter by date range (week, month, custom)
    @Query("SELECT u FROM User u WHERE u.activationDate BETWEEN :start AND :end")
    List<User> findByActivationDateRange(@Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

}
