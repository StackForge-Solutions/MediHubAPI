package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.PharmacyTransactionDetailDto;
import com.MediHubAPI.dto.pharmacy.PharmacyTransactionRowDto;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.pharmacy.PharmacyTransactionNotFoundException;
import com.MediHubAPI.repository.pharmacy.PharmacyStockTransactionRepository;
import com.MediHubAPI.repository.projection.PharmacyTransactionDetailProjection;
import com.MediHubAPI.repository.projection.PharmacyTransactionRowProjection;
import com.MediHubAPI.service.pharmacy.PharmacyTransactionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PharmacyTransactionQueryServiceImpl implements PharmacyTransactionQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> ALLOWED_TRANSACTION_TYPES = Set.of(
            "PURCHASE_RECEIPT",
            "SALE",
            "ADJUSTMENT_IN",
            "ADJUSTMENT_OUT",
            "RETURN_TO_VENDOR",
            "RETURN_FROM_PATIENT",
            "EXPIRED",
            "DAMAGED"
    );

    private final PharmacyStockTransactionRepository pharmacyStockTransactionRepository;

    @Override
    public Page<PharmacyTransactionRowDto> getTransactions(String q,
                                                           Long medicineId,
                                                           Long vendorId,
                                                           String transactionType,
                                                           String batchNo,
                                                           String referenceType,
                                                           Long referenceId,
                                                           LocalDate fromDate,
                                                           LocalDate toDate,
                                                           Integer page,
                                                           Integer size) {
        QueryContext ctx = validateAndNormalize(q, medicineId, vendorId, transactionType, batchNo, referenceType, referenceId,
                fromDate, toDate, page, size);

        Page<PharmacyTransactionRowProjection> transactions = pharmacyStockTransactionRepository.searchTransactions(
                ctx.q(),
                ctx.medicineId(),
                ctx.vendorId(),
                ctx.transactionType(),
                ctx.batchNo(),
                ctx.referenceType(),
                ctx.referenceId(),
                ctx.fromDateTime(),
                ctx.toDateTimeExclusive(),
                PageRequest.of(ctx.page(), ctx.size())
        );

        return transactions.map(this::toRowDto);
    }

    @Override
    public PharmacyTransactionDetailDto getTransaction(Long transactionId) {
        if (transactionId == null || transactionId <= 0) {
            throw new ValidationException("Validation failed", List.of(
                    new ValidationException.ValidationErrorDetail("id", "id must be greater than 0")
            ));
        }

        PharmacyTransactionDetailProjection row = pharmacyStockTransactionRepository.findTransactionDetailById(transactionId)
                .orElseThrow(() -> new PharmacyTransactionNotFoundException(transactionId));

        int qtyIn = nullSafe(row.getQtyIn());
        int qtyOut = nullSafe(row.getQtyOut());
        int balanceAfter = nullSafe(row.getBalanceAfter());

        return PharmacyTransactionDetailDto.builder()
                .transactionId(row.getTransactionId())
                .transactionTime(toInstant(row.getTransactionTime()))
                .transactionType(row.getTransactionType())
                .medicineId(row.getMedicineId())
                .medicineName(row.getMedicineName())
                .batchId(row.getBatchId())
                .batchNo(row.getBatchNo())
                .vendorId(row.getVendorId())
                .vendorName(row.getVendorName())
                .qtyIn(qtyIn)
                .qtyOut(qtyOut)
                .balanceBefore(balanceAfter - qtyIn + qtyOut)
                .balanceAfter(balanceAfter)
                .unitCost(row.getUnitCost())
                .unitPrice(row.getUnitPrice())
                .referenceType(row.getReferenceType())
                .referenceId(row.getReferenceId())
                .referenceNo(row.getReferenceNo())
                .createdBy(row.getCreatedBy())
                .note(row.getNote())
                .build();
    }

    private QueryContext validateAndNormalize(String q,
                                              Long medicineId,
                                              Long vendorId,
                                              String transactionType,
                                              String batchNo,
                                              String referenceType,
                                              Long referenceId,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              Integer page,
                                              Integer size) {
        ValidationErrors errors = new ValidationErrors();

        String normalizedQ = normalizeOptional(q);
        if (normalizedQ != null && normalizedQ.length() < 2) {
            errors.add("q", "q must be at least 2 characters");
        }
        if (medicineId != null && medicineId <= 0) {
            errors.add("medicineId", "medicineId must be greater than 0");
        }
        if (vendorId != null && vendorId <= 0) {
            errors.add("vendorId", "vendorId must be greater than 0");
        }
        if (referenceId != null && referenceId <= 0) {
            errors.add("referenceId", "referenceId must be greater than 0");
        }
        if (page != null && page < 0) {
            errors.add("page", "page must be greater than or equal to 0");
        }
        if (size != null && (size <= 0 || size > MAX_SIZE)) {
            errors.add("size", "size must be between 1 and 100");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            errors.add("fromDate", "fromDate must be before or equal to toDate");
        }

        String normalizedTransactionType = normalizeTransactionType(transactionType, errors);
        String normalizedReferenceType = normalizeUpperOptional(referenceType);
        String normalizedBatchNo = normalizeOptional(batchNo);

        errors.throwIfAny();
        return new QueryContext(
                normalizedQ,
                medicineId,
                vendorId,
                normalizedTransactionType,
                normalizedBatchNo,
                normalizedReferenceType,
                referenceId,
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay(),
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size
        );
    }

    private PharmacyTransactionRowDto toRowDto(PharmacyTransactionRowProjection row) {
        return PharmacyTransactionRowDto.builder()
                .transactionId(row.getTransactionId())
                .transactionTime(toInstant(row.getTransactionTime()))
                .medicineId(row.getMedicineId())
                .medicineName(row.getMedicineName())
                .batchId(row.getBatchId())
                .batchNo(row.getBatchNo())
                .vendorId(row.getVendorId())
                .vendorName(row.getVendorName())
                .transactionType(row.getTransactionType())
                .qtyIn(nullSafe(row.getQtyIn()))
                .qtyOut(nullSafe(row.getQtyOut()))
                .balanceAfter(nullSafe(row.getBalanceAfter()))
                .unitCost(row.getUnitCost())
                .unitPrice(row.getUnitPrice())
                .referenceType(row.getReferenceType())
                .referenceId(row.getReferenceId())
                .referenceNo(row.getReferenceNo())
                .createdBy(row.getCreatedBy())
                .note(row.getNote())
                .build();
    }

    private String normalizeTransactionType(String transactionType, ValidationErrors errors) {
        String normalized = normalizeUpperOptional(transactionType);
        if (normalized != null && !ALLOWED_TRANSACTION_TYPES.contains(normalized)) {
            errors.add("transactionType", "transactionType must be one of " + String.join(", ", ALLOWED_TRANSACTION_TYPES));
            return null;
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpperOptional(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private record QueryContext(String q,
                                Long medicineId,
                                Long vendorId,
                                String transactionType,
                                String batchNo,
                                String referenceType,
                                Long referenceId,
                                LocalDateTime fromDateTime,
                                LocalDateTime toDateTimeExclusive,
                                int page,
                                int size) {
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
