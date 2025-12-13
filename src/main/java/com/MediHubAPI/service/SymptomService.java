package com.MediHubAPI.service;
// package com.MediHubAPI.service;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.model.*;
import com.MediHubAPI.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SymptomService {

    private final SymptomCategoryRepository categoryRepo;
    private final SymptomSubcategoryRepository subcategoryRepo;
    private final SymptomRepository symptomRepo;
    private final EntityManager em;

    // ---------- Category CRUD ----------
    @Transactional
    public SymptomCategoryDto createCategory(SymptomCategoryDto in) {
        SymptomCategory c = new SymptomCategory();
        c.setName(in.name().trim());
        c = categoryRepo.save(c);
        return new SymptomCategoryDto(c.getId(), c.getName());
    }

    public List<SymptomCategoryDto> listCategories() {
        return categoryRepo.findAll().stream()
                .map(c -> new SymptomCategoryDto(c.getId(), c.getName()))
                .toList();
    }

    @Transactional
    public SymptomCategoryDto updateCategory(Long id, SymptomCategoryDto in) {
        SymptomCategory c = categoryRepo.findById(id).orElseThrow();
        c.setName(in.name().trim());
        return new SymptomCategoryDto(c.getId(), c.getName());
    }

    @Transactional
    public void deleteCategory(Long id) {
        // Optional: cascade delete manually (delete children first)
        categoryRepo.deleteById(id);
    }

    // ---------- Subcategory CRUD ----------
    @Transactional
    public SymptomSubcategoryDto createSubcategory(SymptomSubcategoryDto in) {
        SymptomCategory cat = categoryRepo.findById(in.categoryId()).orElseThrow();
        SymptomSubcategory s = new SymptomSubcategory();
        s.setCategory(cat);
        s.setName(in.name().trim());
        s = subcategoryRepo.save(s);
        return new SymptomSubcategoryDto(s.getId(), s.getCategory().getId(), s.getName());
    }


    @Transactional
    public SymptomSubcategoryDto updateSubcategory(Long id, SymptomSubcategoryDto in) {
        SymptomSubcategory s = subcategoryRepo.findById(id).orElseThrow();
        if (!Objects.equals(s.getCategory().getId(), in.categoryId())) {
            s.setCategory(categoryRepo.findById(in.categoryId()).orElseThrow());
        }
        s.setName(in.name().trim());
        return new SymptomSubcategoryDto(s.getId(), s.getCategory().getId(), s.getName());
    }

    @Transactional
    public void deleteSubcategory(Long id) {
        subcategoryRepo.deleteById(id);
    }

    // ---------- Symptom CRUD ----------
    @Transactional
    public SymptomDto createSymptom(SymptomDto in) {
        SymptomSubcategory sub = subcategoryRepo.findById(in.subcategoryId()).orElseThrow();
        Symptom sm = new Symptom();
        sm.setSubcategory(sub);
        sm.setName(in.name().trim());
        sm = symptomRepo.save(sm);
        return new SymptomDto(sm.getId(), sm.getSubcategory().getId(), sm.getName());
    }


    @Transactional
    public SymptomDto updateSymptom(Long id, SymptomDto in) {
        Symptom sm = symptomRepo.findById(id).orElseThrow();
        if (!Objects.equals(sm.getSubcategory().getId(), in.subcategoryId())) {
            sm.setSubcategory(subcategoryRepo.findById(in.subcategoryId()).orElseThrow());
        }
        sm.setName(in.name().trim());
        return new SymptomDto(sm.getId(), sm.getSubcategory().getId(), sm.getName());
    }

    @Transactional
    public void deleteSymptom(Long id) {
        symptomRepo.deleteById(id);
    }

    // ---------- Bulk import (idempotent upsert-ish) ----------
    @Transactional
    public void importCatalog(SymptomCatalogDto dto, int flushEvery) {
        int counter = 0;
        for (var entryCat : dto.categories().entrySet()) {
            String categoryName = entryCat.getKey().trim();
            SymptomCategory category = categoryRepo.findByNameIgnoreCase(categoryName)
                    .orElseGet(() -> categoryRepo.save(newCategory(categoryName)));

            for (var entrySub : entryCat.getValue().entrySet()) {
                String subName = entrySub.getKey().trim();
                SymptomSubcategory sub = subcategoryRepo
                        .findByCategoryIdAndNameIgnoreCase(category.getId(), subName)
                        .orElseGet(() -> subcategoryRepo.save(newSub(category, subName)));

                for (String symptomNameRaw : entrySub.getValue()) {
                    String symptomName = symptomNameRaw.trim();
                    if (symptomName.isEmpty()) continue;

                    symptomRepo.findBySubcategoryIdAndNameIgnoreCase(sub.getId(), symptomName)
                            .orElseGet(() -> {
                                Symptom s = new Symptom();
                                s.setSubcategory(sub);
                                s.setName(symptomName);
                                return symptomRepo.save(s);
                            });

                    // batch flush & clear to keep memory low on very large imports
                    if (++counter % flushEvery == 0) {
                        em.flush(); em.clear();
                    }
                }
            }
        }
    }

    private SymptomCategory newCategory(String name) {
        SymptomCategory c = new SymptomCategory();
        c.setName(name);
        return c;
    }
    private SymptomSubcategory newSub(SymptomCategory c, String name) {
        SymptomSubcategory s = new SymptomSubcategory();
        s.setCategory(c);
        s.setName(name);
        return s;
    }

    public List<Map<String,Object>> searchSymptoms(String q, String category, int limit) {
        List<Symptom> list;
        if (category != null && !category.isBlank()) {
            list = symptomRepo.findTop10ByNameIgnoreCaseContainingAndSubcategory_Category_Name(q, category);
        } else {
            list = symptomRepo.findTop10ByNameIgnoreCaseContaining(q);
        }

        // Convert to lightweight map for typeahead
        return list.stream().map(s -> {
            Map<String,Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("subcategory", s.getSubcategory().getName());
            map.put("category", s.getSubcategory().getCategory().getName());
            return map;
        }).toList();

    }
}
