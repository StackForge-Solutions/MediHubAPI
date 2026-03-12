package com.MediHubAPI.controller.pharmacy;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.PageResponse;
import com.MediHubAPI.dto.pharmacy.PharmacyTransactionDetailDto;
import com.MediHubAPI.dto.pharmacy.PharmacyTransactionRowDto;
import com.MediHubAPI.service.pharmacy.PharmacyTransactionQueryService;
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

import java.time.LocalDate;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/pharmacy/transactions")
@Slf4j
public class PharmacyTransactionController {

    private final PharmacyTransactionQueryService pharmacyTransactionQueryService;

    @GetMapping
    public PageResponse<PharmacyTransactionRowDto> getTransactions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Positive Long medicineId,
            @RequestParam(required = false) @Positive Long vendorId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) @Positive Long referenceId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("API call: pharmacy transactions q={}, medicineId={}, vendorId={}, transactionType={}, batchNo={}, referenceType={}, referenceId={}, fromDate={}, toDate={}, page={}, size={}",
                q, medicineId, vendorId, transactionType, batchNo, referenceType, referenceId, fromDate, toDate, page, size);
        return toPageResponse(pharmacyTransactionQueryService.getTransactions(
                q, medicineId, vendorId, transactionType, batchNo, referenceType, referenceId, fromDate, toDate, page, size
        ));
    }

    @GetMapping("/{id}")
    public DataResponse<PharmacyTransactionDetailDto> getTransaction(
            @PathVariable("id") @Positive Long transactionId
    ) {
        log.info("API call: pharmacy transaction detail id={}", transactionId);
        return new DataResponse<>(pharmacyTransactionQueryService.getTransaction(transactionId));
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
