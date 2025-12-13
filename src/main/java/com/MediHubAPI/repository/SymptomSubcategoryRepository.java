package com.MediHubAPI.repository;

import com.MediHubAPI.model.SymptomSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SymptomSubcategoryRepository extends JpaRepository<SymptomSubcategory, Long> {
    Optional<SymptomSubcategory> findByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    List<SymptomSubcategory> findByCategoryId(Long categoryId);
}
