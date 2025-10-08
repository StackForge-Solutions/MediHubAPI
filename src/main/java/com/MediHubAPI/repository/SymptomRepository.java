package com.MediHubAPI.repository;

import com.MediHubAPI.model.Symptom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SymptomRepository extends JpaRepository<Symptom, Long>, JpaSpecificationExecutor<Symptom> {
    Optional<Symptom> findBySubcategoryIdAndNameIgnoreCase(Long subcategoryId, String name);



    List<Symptom> findTop10ByNameIgnoreCaseContaining(String name);
    List<Symptom> findTop10ByNameIgnoreCaseContainingAndSubcategory_Category_Name(String name, String category);

}
