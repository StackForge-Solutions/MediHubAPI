package com.MediHubAPI.repository;


import com.MediHubAPI.model.VisitSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitSummaryRepository extends JpaRepository<VisitSummary, Long> {
}
