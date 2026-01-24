package com.MediHubAPI.repository;

import com.MediHubAPI.model.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Long> {

    @Query("select distinct s.department from Specialization s where s.department is not null and trim(s.department) <> '' order by s.department")
    List<String> findDistinctDepartments();
}
