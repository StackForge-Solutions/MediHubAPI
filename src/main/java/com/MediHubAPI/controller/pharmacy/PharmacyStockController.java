package com.MediHubAPI.controller.pharmacy;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.PageResponse;
import com.MediHubAPI.dto.pharmacy.*;
import com.MediHubAPI.service.pharmacy.PharmacyStockQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/pharmacy/stocks")
@Slf4j
public class PharmacyStockController {

    private final PharmacyStockQueryService pharmacyStockQueryService;

    @GetMapping
    public PageResponse<ManageStockRowDto> getManageStocks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false, defaultValue = "medicineName,asc") String sort,
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly,
            @RequestParam(required = false, defaultValue = "false") Boolean lowStockOnly,
            @RequestParam(required = false) @Min(0) Integer expiringInDays,
            @RequestParam(required = false) @Positive Long vendorId,
            @RequestParam(required = false) String form
    ) {
        log.info("API call: manage stocks q={}, page={}, size={}, sort={}, inStockOnly={}, lowStockOnly={}, expiringInDays={}, vendorId={}, form={}",
                q, page, size, sort, inStockOnly, lowStockOnly, expiringInDays, vendorId, form);
        return toPageResponse(pharmacyStockQueryService.getManageStocks(
                q, page, size, sort, inStockOnly, lowStockOnly, expiringInDays, vendorId, form
        ));
    }

    @GetMapping("/summary")
    public DataResponse<StockSummaryDto> getStockSummary(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Positive Long vendorId,
            @RequestParam(required = false) String form
    ) {
        log.info("API call: stock summary q={}, vendorId={}, form={}", q, vendorId, form);
        return new DataResponse<>(pharmacyStockQueryService.getStockSummary(q, vendorId, form));
    }

    @GetMapping("/{medicineId}")
    public DataResponse<MedicineStockDetailDto> getStockDetail(
            @PathVariable @Positive Long medicineId
    ) {
        log.info("API call: stock detail medicineId={}", medicineId);
        return new DataResponse<>(pharmacyStockQueryService.getStockDetail(medicineId));
    }

    @GetMapping("/{medicineId}/batches")
    public PageResponse<MedicineStockBatchDto> getStockBatches(
            @PathVariable @Positive Long medicineId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeExpired,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("API call: stock batches medicineId={}, includeExpired={}, page={}, size={}",
                medicineId, includeExpired, page, size);
        return toPageResponse(pharmacyStockQueryService.getStockBatches(medicineId, includeExpired, page, size));
    }

    @GetMapping("/{medicineId}/transactions")
    public PageResponse<MedicineStockTransactionDto> getStockTransactions(
            @PathVariable @Positive Long medicineId,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("API call: stock transactions medicineId={}, page={}, size={}", medicineId, page, size);
        return toPageResponse(pharmacyStockQueryService.getStockTransactions(medicineId, page, size));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
