package com.MediHubAPI.repository;

import com.MediHubAPI.model.ChiefComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChiefComplaintRepository extends JpaRepository<ChiefComplaint, Long> {
    List<ChiefComplaint> findByVisitSummary_Id(Long visitSummaryId);
}
