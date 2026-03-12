package com.MediHubAPI.service.pharmacy;

import com.MediHubAPI.dto.pharmacy.*;
import org.springframework.data.domain.Page;

public interface PharmacyVendorService {
    Page<PharmacyVendorRowDto> getVendors(String q, Boolean active, Integer page, Integer size, String sort);

    PharmacyVendorDetailDto createVendor(PharmacyVendorUpsertRequest request);

    PharmacyVendorDetailDto updateVendor(Long vendorId, PharmacyVendorUpsertRequest request);

    PharmacyVendorDetailDto getVendor(Long vendorId);

    Page<PharmacyVendorPurchaseOrderRowDto> getVendorPurchaseOrders(Long vendorId, Integer page, Integer size);
}
