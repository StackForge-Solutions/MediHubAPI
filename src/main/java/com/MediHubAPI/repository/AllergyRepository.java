package com.MediHubAPI.repository;

import com.MediHubAPI.model.Allergy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {
    Optional<Allergy> findByVisitSummary_Id(Long visitSummaryId);
}
