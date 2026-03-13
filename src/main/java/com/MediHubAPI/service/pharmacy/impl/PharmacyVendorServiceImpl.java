package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.*;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.pharmacy.DuplicateVendorCodeException;
import com.MediHubAPI.exception.pharmacy.PharmacyVendorNotFoundException;
import com.MediHubAPI.model.pharmacy.PharmacyVendor;
import com.MediHubAPI.repository.pharmacy.PharmacyVendorRepository;
import com.MediHubAPI.repository.projection.PharmacyVendorRowProjection;
import com.MediHubAPI.service.pharmacy.PharmacyVendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PharmacyVendorServiceImpl implements PharmacyVendorService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "vendorName", "vendorCode", "city", "paymentTermsDays", "active", "lastPurchaseDate"
    );

    private final PharmacyVendorRepository pharmacyVendorRepository;

    @Override
    public Page<PharmacyVendorRowDto> getVendors(String q, Boolean active, Integer page, Integer size, String sort) {
        QueryContext ctx = validateAndNormalizeQuery(q, page, size, sort);
        Page<PharmacyVendorRowProjection> rows = pharmacyVendorRepository.searchVendors(
                ctx.q(),
                active,
                ctx.sortField(),
                ctx.sortDir(),
                PageRequest.of(ctx.page(), ctx.size())
        );

        return rows.map(this::toRowDto);
    }

    @Override
    @Transactional
    public PharmacyVendorDetailDto createVendor(PharmacyVendorUpsertRequest request) {
        String vendorCode = normalizeRequired(request.getVendorCode());
        if (pharmacyVendorRepository.existsByVendorCodeIgnoreCase(vendorCode)) {
            throw new DuplicateVendorCodeException(vendorCode);
        }

        PharmacyVendor vendor = PharmacyVendor.builder()
                .vendorCode(vendorCode)
                .vendorName(normalizeRequired(request.getVendorName()))
                .contactPerson(normalizeOptional(request.getContactPerson()))
                .phone(normalizeOptional(request.getPhone()))
                .email(normalizeOptional(request.getEmail()))
                .gstNo(normalizeOptional(request.getGstNo()))
                .drugLicenseNo(normalizeOptional(request.getDrugLicenseNo()))
                .addressLine1(normalizeOptional(request.getAddressLine1()))
                .addressLine2(normalizeOptional(request.getAddressLine2()))
                .city(normalizeOptional(request.getCity()))
                .state(normalizeOptional(request.getState()))
                .pincode(normalizeOptional(request.getPincode()))
                .paymentTermsDays(request.getPaymentTermsDays() == null ? 0 : request.getPaymentTermsDays())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        PharmacyVendor saved = pharmacyVendorRepository.save(vendor);
        log.info("Created pharmacy vendor id={}, code={}", saved.getId(), saved.getVendorCode());
        return toDetailDto(saved);
    }

    @Override
    @Transactional
    public PharmacyVendorDetailDto updateVendor(Long vendorId, PharmacyVendorUpsertRequest request) {
        PharmacyVendor vendor = getVendorEntityOrThrow(vendorId);
        String vendorCode = normalizeRequired(request.getVendorCode());
        if (pharmacyVendorRepository.existsByVendorCodeIgnoreCaseAndIdNot(vendorCode, vendorId)) {
            throw new DuplicateVendorCodeException(vendorCode);
        }

        vendor.setVendorCode(vendorCode);
        vendor.setVendorName(normalizeRequired(request.getVendorName()));
        vendor.setContactPerson(normalizeOptional(request.getContactPerson()));
        vendor.setPhone(normalizeOptional(request.getPhone()));
        vendor.setEmail(normalizeOptional(request.getEmail()));
        vendor.setGstNo(normalizeOptional(request.getGstNo()));
        vendor.setDrugLicenseNo(normalizeOptional(request.getDrugLicenseNo()));
        vendor.setAddressLine1(normalizeOptional(request.getAddressLine1()));
        vendor.setAddressLine2(normalizeOptional(request.getAddressLine2()));
        vendor.setCity(normalizeOptional(request.getCity()));
        vendor.setState(normalizeOptional(request.getState()));
        vendor.setPincode(normalizeOptional(request.getPincode()));
        vendor.setPaymentTermsDays(request.getPaymentTermsDays() == null ? 0 : request.getPaymentTermsDays());
        vendor.setActive(request.getActive() == null ? vendor.getActive() : request.getActive());

        PharmacyVendor saved = pharmacyVendorRepository.save(vendor);
        log.info("Updated pharmacy vendor id={}, code={}", saved.getId(), saved.getVendorCode());
        return toDetailDto(saved);
    }

    @Override
    public PharmacyVendorDetailDto getVendor(Long vendorId) {
        return toDetailDto(getVendorEntityOrThrow(vendorId));
    }

    @Override
    public Page<PharmacyVendorPurchaseOrderRowDto> getVendorPurchaseOrders(Long vendorId, Integer page, Integer size) {
        getVendorEntityOrThrow(vendorId);
        int safePage = page == null ? DEFAULT_PAGE : page;
        int safeSize = size == null ? DEFAULT_SIZE : size;
        return new PageImpl<>(List.of(), PageRequest.of(safePage, safeSize), 0);
    }

    private QueryContext validateAndNormalizeQuery(String q, Integer page, Integer size, String sort) {
        ValidationErrors errors = new ValidationErrors();

        String normalizedQ = normalizeOptional(q);
        if (normalizedQ != null && normalizedQ.length() < 2) {
            errors.add("q", "q must be at least 2 characters");
        }

        String safeSort = (sort == null || sort.isBlank()) ? "vendorName,asc" : sort.trim();
        String[] parts = safeSort.split(",");
        String sortField = parts[0].trim();
        String sortDir = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";

        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            errors.add("sort", "unsupported sort field");
        }
        if (!sortDir.equals("asc") && !sortDir.equals("desc")) {
            errors.add("sort", "sort direction must be asc or desc");
        }

        errors.throwIfAny();
        return new QueryContext(
                normalizedQ,
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size,
                sortField,
                sortDir
        );
    }

    private PharmacyVendor getVendorEntityOrThrow(Long vendorId) {
        return pharmacyVendorRepository.findById(vendorId)
                .orElseThrow(() -> new PharmacyVendorNotFoundException(vendorId));
    }

    private PharmacyVendorRowDto toRowDto(PharmacyVendorRowProjection row) {
        return PharmacyVendorRowDto.builder()
                .vendorId(row.getVendorId())
                .vendorCode(row.getVendorCode())
                .vendorName(row.getVendorName())
                .contactPerson(row.getContactPerson())
                .phone(row.getPhone())
                .email(row.getEmail())
                .gstNo(row.getGstNo())
                .city(row.getCity())
                .paymentTermsDays(row.getPaymentTermsDays() == null ? 0 : row.getPaymentTermsDays())
                .active(Boolean.TRUE.equals(row.getActive()))
                .outstandingPurchaseOrders(0)
                .lastPurchaseDate(null)
                .build();
    }

    private PharmacyVendorDetailDto toDetailDto(PharmacyVendor vendor) {
        return PharmacyVendorDetailDto.builder()
                .vendorId(vendor.getId())
                .vendorCode(vendor.getVendorCode())
                .vendorName(vendor.getVendorName())
                .contactPerson(vendor.getContactPerson())
                .phone(vendor.getPhone())
                .email(vendor.getEmail())
                .gstNo(vendor.getGstNo())
                .drugLicenseNo(vendor.getDrugLicenseNo())
                .addressLine1(vendor.getAddressLine1())
                .addressLine2(vendor.getAddressLine2())
                .city(vendor.getCity())
                .state(vendor.getState())
                .pincode(vendor.getPincode())
                .paymentTermsDays(vendor.getPaymentTermsDays() == null ? 0 : vendor.getPaymentTermsDays())
                .active(Boolean.TRUE.equals(vendor.getActive()))
                .stats(PharmacyVendorStatsDto.builder()
                        .totalPurchaseOrders(0)
                        .pendingPurchaseOrders(0)
                        .totalPurchaseValue(BigDecimal.ZERO)
                        .lastPurchaseDate(null)
                        .build())
                .build();
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record QueryContext(String q, int page, int size, String sortField, String sortDir) {
    }

    private static class ValidationErrors {
        private final java.util.ArrayList<ValidationException.ValidationErrorDetail> details = new java.util.ArrayList<>();

        void add(String field, String message) {
            details.add(new ValidationException.ValidationErrorDetail(field, message));
        }

        void throwIfAny() {
            if (!details.isEmpty()) {
                throw new ValidationException("Validation failed", List.copyOf(details));
            }
        }
    }
}
