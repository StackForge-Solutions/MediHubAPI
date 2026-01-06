package com.MediHubAPI.controller.pharmacy;

import com.MediHubAPI.dto.pharmacy.MedicineSearchResponse;
import com.MediHubAPI.enums.pharmacy.MedicineForm;
import com.MediHubAPI.enums.pharmacy.MedicineSearchMode;
import com.MediHubAPI.service.pharmacy.MedicineSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pharmacy/medicines")
@RequiredArgsConstructor
@Slf4j
public class MedicineSearchController {

    private final MedicineSearchService medicineSearchService;

    @GetMapping("/search")
    public MedicineSearchResponse searchMedicines(
            @RequestParam MedicineSearchMode mode,
            @RequestParam(required = false) MedicineForm form,
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly
    ) {
        return medicineSearchService.search(mode, form, q, limit, inStockOnly);
    }
}
