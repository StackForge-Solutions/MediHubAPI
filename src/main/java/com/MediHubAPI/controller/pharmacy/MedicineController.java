package com.MediHubAPI.controller.pharmacy;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.pharmacy.MedicineSearchResponse;
import com.MediHubAPI.dto.pharmacy.MedicineStockDto;
import com.MediHubAPI.enums.pharmacy.MedicineForm;
import com.MediHubAPI.enums.pharmacy.MedicineSearchMode;
 import com.MediHubAPI.service.pharmacy.MedicineStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pharmacy/medicines")
@RequiredArgsConstructor
@Slf4j
public class MedicineController {

    private final MedicineStockService medicineStockService;

    @GetMapping("/search")
    public MedicineSearchResponse searchMedicines(
            @RequestParam MedicineSearchMode mode,
            @RequestParam(required = false) MedicineForm form,
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly
    ) {
        return medicineStockService.search(mode, form, q, limit, inStockOnly);
    }

    /** ✅ GET /api/pharmacy/medicines/{id}/stock */
    @GetMapping("/{id}/stock")
    public DataResponse<MedicineStockDto> getStock(@PathVariable("id") Long id) {
        log.info("API call: getStock medicineId={}", id);
        return new DataResponse<>(medicineStockService.getStock(id));
    }
}
