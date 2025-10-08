package com.MediHubAPI.controller;
// package com.MediHubAPI.controller;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.service.SymptomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/symptoms")
public class SymptomController {

    private final SymptomService service;

    // ---- Category ----
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<SymptomCategoryDto>> createCategory(
            @RequestBody SymptomCategoryDto in) {
        var out = service.createCategory(in);
        return ResponseEntity.created(URI.create("/api/symptoms/categories/" + out.id()))
                .body(ApiResponse.created(out, "/api/symptoms/categories", "Category created"));
    }

    @GetMapping("/categories")
    public ApiResponse<List<SymptomCategoryDto>> listCategories() {
        return ApiResponse.success(service.listCategories(), "/api/symptoms/categories", "OK");
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<SymptomCategoryDto> updateCategory(@PathVariable Long id,
                                                          @RequestBody SymptomCategoryDto in) {
        return ApiResponse.success(service.updateCategory(id, in),
                "/api/symptoms/categories/" + id, "Category updated");
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ApiResponse.ok("Category deleted", "/api/symptoms/categories/" + id);
    }

    // ---- Subcategory ----
    @PostMapping("/subcategories")
    public ResponseEntity<ApiResponse<SymptomSubcategoryDto>> createSubcategory(
            @RequestBody SymptomSubcategoryDto in) {
        var out = service.createSubcategory(in);
        return ResponseEntity.created(URI.create("/api/symptoms/subcategories/" + out.id()))
                .body(ApiResponse.created(out, "/api/symptoms/subcategories", "Subcategory created"));
    }


    @PutMapping("/subcategories/{id}")
    public ApiResponse<SymptomSubcategoryDto> updateSubcategory(@PathVariable Long id,
                                                                @RequestBody SymptomSubcategoryDto in) {
        return ApiResponse.success(service.updateSubcategory(id, in),
                "/api/symptoms/subcategories/" + id, "Subcategory updated");
    }

    @DeleteMapping("/subcategories/{id}")
    public ApiResponse<Void> deleteSubcategory(@PathVariable Long id) {
        service.deleteSubcategory(id);
        return ApiResponse.ok("Subcategory deleted", "/api/symptoms/subcategories/" + id);
    }

    // ---- Symptom ----
    @PostMapping
    public ResponseEntity<ApiResponse<SymptomDto>> createSymptom(
            @RequestBody SymptomDto in) {
        var out = service.createSymptom(in);
        return ResponseEntity.created(URI.create("/api/symptoms/" + out.id()))
                .body(ApiResponse.created(out, "/api/symptoms", "Symptom created"));
    }


    @PutMapping("/{id}")
    public ApiResponse<SymptomDto> updateSymptom(@PathVariable Long id,
                                                 @RequestBody SymptomDto in) {
        return ApiResponse.success(service.updateSymptom(id, in),
                "/api/symptoms/" + id, "Symptom updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSymptom(@PathVariable Long id) {
        service.deleteSymptom(id);
        return ApiResponse.ok("Symptom deleted", "/api/symptoms/" + id);
    }

    // ---- Bulk Import (hierarchical JSON) ----
    @PostMapping("/import")
    public ApiResponse<String> importCatalog(@RequestBody SymptomCatalogDto catalog) {
        service.importCatalog(catalog, 200); // flush every 200 rows
        return ApiResponse.created("OK", "/api/symptoms/import", "Catalog imported");
    }


    @GetMapping("/search")
    public ApiResponse<List<Map<String,Object>>> typeahead(
            @RequestParam String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Map<String,Object>> results = service.searchSymptoms(q, category, limit);
        return ApiResponse.success(results, "/api/symptoms/search", "Typeahead results");
    }
}
