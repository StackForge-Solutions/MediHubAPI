package com.MediHubAPI.controller.pharmacy;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.PageResponse;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiptHistoryRowDto;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveRequest;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveResponseDto;
import com.MediHubAPI.service.pharmacy.PharmacyPurchaseOrderReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/pharmacy/purchase-orders")
@Slf4j
public class PharmacyPurchaseOrderReceiptController {

    private final PharmacyPurchaseOrderReceiptService pharmacyPurchaseOrderReceiptService;

    @PostMapping("/{id}/receive")
    public DataResponse<PurchaseOrderReceiveResponseDto> receive(
            @PathVariable("id") @Positive Long purchaseOrderId,
            @Valid @RequestBody PurchaseOrderReceiveRequest request
    ) {
        log.info("API call: receive stock purchaseOrderId={}, itemCount={}", purchaseOrderId, request.getItems().size());
        return new DataResponse<>(pharmacyPurchaseOrderReceiptService.receive(purchaseOrderId, request));
    }

    @GetMapping("/{id}/receipts")
    public PageResponse<PurchaseOrderReceiptHistoryRowDto> getReceiptHistory(
            @PathVariable("id") @Positive Long purchaseOrderId,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("API call: receipt history purchaseOrderId={}, page={}, size={}", purchaseOrderId, page, size);
        return toPageResponse(pharmacyPurchaseOrderReceiptService.getReceiptHistory(purchaseOrderId, page, size));
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
