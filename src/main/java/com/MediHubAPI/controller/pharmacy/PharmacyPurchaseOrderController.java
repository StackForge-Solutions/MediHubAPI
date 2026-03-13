package com.MediHubAPI.controller.pharmacy;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.PageResponse;
import com.MediHubAPI.dto.pharmacy.*;
import com.MediHubAPI.service.pharmacy.PharmacyPurchaseOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/pharmacy/purchase-orders")
@Slf4j
public class PharmacyPurchaseOrderController {

    private final PharmacyPurchaseOrderService purchaseOrderService;

    @GetMapping
    public PageResponse<PurchaseOrderRowDto> getPurchaseOrders(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Positive Long vendorId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false, defaultValue = "orderDate,desc") String sort
    ) {
        log.info("API call: purchase orders q={}, status={}, vendorId={}, fromDate={}, toDate={}, page={}, size={}, sort={}",
                q, status, vendorId, fromDate, toDate, page, size, sort);
        return toPageResponse(purchaseOrderService.getPurchaseOrders(q, status, vendorId, fromDate, toDate, page, size, sort));
    }

    @PostMapping
    public DataResponse<PurchaseOrderDetailDto> createPurchaseOrder(@Valid @RequestBody PurchaseOrderCreateRequest request) {
        log.info("API call: create purchase order vendorId={}, itemCount={}", request.getVendorId(), request.getItems().size());
        return new DataResponse<>(purchaseOrderService.createPurchaseOrder(request));
    }

    @PutMapping("/{id}")
    public DataResponse<PurchaseOrderDetailDto> updatePurchaseOrder(
            @PathVariable("id") @Positive Long purchaseOrderId,
            @Valid @RequestBody PurchaseOrderUpdateRequest request
    ) {
        log.info("API call: update purchase order id={}, itemCount={}", purchaseOrderId, request.getItems().size());
        return new DataResponse<>(purchaseOrderService.updatePurchaseOrder(purchaseOrderId, request));
    }

    @GetMapping("/{id}")
    public DataResponse<PurchaseOrderDetailDto> getPurchaseOrder(@PathVariable("id") @Positive Long purchaseOrderId) {
        log.info("API call: purchase order detail id={}", purchaseOrderId);
        return new DataResponse<>(purchaseOrderService.getPurchaseOrder(purchaseOrderId));
    }

    @PostMapping("/{id}/approve")
    public DataResponse<PurchaseOrderActionResponseDto> approvePurchaseOrder(@PathVariable("id") @Positive Long purchaseOrderId) {
        log.info("API call: approve purchase order id={}", purchaseOrderId);
        return new DataResponse<>(purchaseOrderService.approvePurchaseOrder(purchaseOrderId));
    }

    @PostMapping("/{id}/cancel")
    public DataResponse<PurchaseOrderActionResponseDto> cancelPurchaseOrder(@PathVariable("id") @Positive Long purchaseOrderId) {
        log.info("API call: cancel purchase order id={}", purchaseOrderId);
        return new DataResponse<>(purchaseOrderService.cancelPurchaseOrder(purchaseOrderId));
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
