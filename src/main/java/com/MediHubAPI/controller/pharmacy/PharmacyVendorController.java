package com.MediHubAPI.controller.pharmacy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.PageResponse;
import com.MediHubAPI.dto.pharmacy.PharmacyVendorDetailDto;
import com.MediHubAPI.dto.pharmacy.PharmacyVendorPurchaseOrderRowDto;
import com.MediHubAPI.dto.pharmacy.PharmacyVendorRowDto;
import com.MediHubAPI.dto.pharmacy.PharmacyVendorUpsertRequest;
import com.MediHubAPI.service.pharmacy.PharmacyVendorService;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/pharmacy/vendors")
@Slf4j
public class PharmacyVendorController {

    private final PharmacyVendorService pharmacyVendorService;

    @GetMapping
    public PageResponse<PharmacyVendorRowDto> getVendors(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(200) Integer size,
            @RequestParam(required = false, defaultValue = "vendorName,asc") String sort
    ) {
        log.info("API call: pharmacy vendors q={}, active={}, page={}, size={}, sort={}",
                q, active, page, size, sort);
        return toPageResponse(pharmacyVendorService.getVendors(q, active, page, size, sort));
    }

    @PostMapping
    public DataResponse<PharmacyVendorDetailDto> createVendor(@Valid @RequestBody PharmacyVendorUpsertRequest request) {
        log.info("API call: create pharmacy vendor code={}", request.getVendorCode());
        return new DataResponse<>(pharmacyVendorService.createVendor(request));
    }

    @PutMapping("/{vendorId}")
    public DataResponse<PharmacyVendorDetailDto> updateVendor(
            @PathVariable @Positive Long vendorId,
            @Valid @RequestBody PharmacyVendorUpsertRequest request
    ) {
        log.info("API call: update pharmacy vendor id={}, code={}", vendorId, request.getVendorCode());
        return new DataResponse<>(pharmacyVendorService.updateVendor(vendorId, request));
    }

    @GetMapping("/{vendorId}")
    public DataResponse<PharmacyVendorDetailDto> getVendor(@PathVariable @Positive Long vendorId) {
        log.info("API call: pharmacy vendor detail id={}", vendorId);
        return new DataResponse<>(pharmacyVendorService.getVendor(vendorId));
    }

    @GetMapping("/{vendorId}/purchase-orders")
    public PageResponse<PharmacyVendorPurchaseOrderRowDto> getVendorPurchaseOrders(
            @PathVariable @Positive Long vendorId,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(200) Integer size
    ) {
        log.info("API call: pharmacy vendor purchase orders vendorId={}, page={}, size={}", vendorId, page, size);
        return toPageResponse(pharmacyVendorService.getVendorPurchaseOrders(vendorId, page, size));
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
