package com.MediHubAPI.repository;

import com.MediHubAPI.model.PrescribedTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescribedTestRepository extends JpaRepository<PrescribedTest, Long> {
    List<PrescribedTest> findByVisitSummary_Id(Long visitSummaryId);
    List<PrescribedTest> findByVisitSummary_IdIn(List<Long> visitSummaryIds);
}
