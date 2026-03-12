package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.*;
import com.MediHubAPI.enums.pharmacy.MedicineForm;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.pharmacy.MedicineNotFoundException;
import com.MediHubAPI.model.mdm.MdmMedicine;
import com.MediHubAPI.model.pharmacy.PharmacyStock;
import com.MediHubAPI.repository.pharmacy.MdmMedicineRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockBatchRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockTransactionRepository;
import com.MediHubAPI.repository.projection.ManageStockRowProjection;
import com.MediHubAPI.repository.projection.MedicineBatchProjection;
import com.MediHubAPI.repository.projection.MedicineTransactionProjection;
import com.MediHubAPI.repository.projection.StockSummaryProjection;
import com.MediHubAPI.service.pharmacy.PharmacyStockQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PharmacyStockQueryServiceImpl implements PharmacyStockQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int DEFAULT_EXPIRY_WARNING_DAYS = 30;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "medicineName", "brand", "form", "availableQty", "nearestExpiryDate", "stockValue"
    );

    private final MdmMedicineRepository medicineRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PharmacyStockBatchRepository pharmacyStockBatchRepository;
    private final PharmacyStockTransactionRepository pharmacyStockTransactionRepository;

    @Override
    public Page<ManageStockRowDto> getManageStocks(String q,
                                                   Integer page,
                                                   Integer size,
                                                   String sort,
                                                   Boolean inStockOnly,
                                                   Boolean lowStockOnly,
                                                   Integer expiringInDays,
                                                   Long vendorId,
                                                   String form) {

        QueryContext ctx = validateAndNormalize(q, page, size, sort, expiringInDays, vendorId, form, false);

        Page<ManageStockRowProjection> rows = pharmacyStockRepository.searchManageStocks(
                ctx.q(),
                ctx.form(),
                Boolean.TRUE.equals(inStockOnly),
                Boolean.TRUE.equals(lowStockOnly),
                ctx.expiringInDays(),
                ctx.vendorId(),
                ctx.sortField(),
                ctx.sortDir(),
                PageRequest.of(ctx.page(), ctx.size())
        );

        int expiryThreshold = ctx.expiringInDays() == null ? DEFAULT_EXPIRY_WARNING_DAYS : ctx.expiringInDays();
        return rows.map(row -> toManageStockRowDto(row, expiryThreshold));
    }

    @Override
    public StockSummaryDto getStockSummary(String q, Long vendorId, String form) {
        QueryContext ctx = validateAndNormalize(q, DEFAULT_PAGE, DEFAULT_SIZE, "medicineName,asc", null, vendorId, form, true);
        StockSummaryProjection summary = pharmacyStockRepository.summarizeManageStocks(ctx.q(), ctx.vendorId(), ctx.form());
        if (summary == null) {
            return StockSummaryDto.builder()
                    .totalMedicines(0)
                    .lowStockCount(0)
                    .outOfStockCount(0)
                    .expiringSoonCount(0)
                    .stockValue(BigDecimal.ZERO)
                    .build();
        }

        return StockSummaryDto.builder()
                .totalMedicines(nullSafe(summary.getTotalMedicines()))
                .lowStockCount(nullSafe(summary.getLowStockCount()))
                .outOfStockCount(nullSafe(summary.getOutOfStockCount()))
                .expiringSoonCount(nullSafe(summary.getExpiringSoonCount()))
                .stockValue(nullSafe(summary.getStockValue()))
                .build();
    }

    @Override
    public MedicineStockDetailDto getStockDetail(Long medicineId) {
        MdmMedicine medicine = getActiveMedicineOrThrow(medicineId);
        PharmacyStock stock = pharmacyStockRepository.findByMedicine_Id(medicineId).orElse(null);

        Integer availableQty = stock == null || stock.getAvailableQty() == null ? 0 : stock.getAvailableQty();
        Integer reservedQty = stock == null || stock.getReservedQty() == null ? 0 : stock.getReservedQty();
        Integer reorderLevel = stock == null || stock.getReorderLevel() == null ? 0 : stock.getReorderLevel();

        LocalDate nearestExpiryDate = pharmacyStockBatchRepository.findNearestExpiryDate(medicineId);
        BigDecimal stockValue = nullSafe(pharmacyStockBatchRepository.computeStockValue(medicineId));

        return MedicineStockDetailDto.builder()
                .medicineId(medicine.getId())
                .medicineCode(medicine.getCode() == null || medicine.getCode().isBlank()
                        ? "MED-" + medicine.getId()
                        : medicine.getCode())
                .medicineName(medicine.getBrand())
                .brand(medicine.getBrand())
                .form(medicine.getForm())
                .composition(medicine.getComposition())
                .availableQty(availableQty)
                .reservedQty(reservedQty)
                .reorderLevel(reorderLevel)
                .nearestExpiryDate(nearestExpiryDate)
                .stockValue(stockValue)
                .lowStock(availableQty > 0 && availableQty <= reorderLevel)
                .build();
    }

    @Override
    public Page<MedicineStockBatchDto> getStockBatches(Long medicineId, Boolean includeExpired, Integer page, Integer size) {
        getActiveMedicineOrThrow(medicineId);
        int safePage = page == null ? DEFAULT_PAGE : page;
        int safeSize = size == null ? DEFAULT_SIZE : size;

        Page<MedicineBatchProjection> batches = pharmacyStockBatchRepository.findBatchPageByMedicineId(
                medicineId,
                Boolean.TRUE.equals(includeExpired),
                PageRequest.of(safePage, safeSize)
        );

        return batches.map(this::toMedicineStockBatchDto);
    }

    @Override
    public Page<MedicineStockTransactionDto> getStockTransactions(Long medicineId, Integer page, Integer size) {
        getActiveMedicineOrThrow(medicineId);
        int safePage = page == null ? DEFAULT_PAGE : page;
        int safeSize = size == null ? DEFAULT_SIZE : size;

        Page<MedicineTransactionProjection> transactions = pharmacyStockTransactionRepository.findByMedicineId(
                medicineId,
                PageRequest.of(safePage, safeSize)
        );

        return transactions.map(this::toMedicineStockTransactionDto);
    }

    private QueryContext validateAndNormalize(String q,
                                              Integer page,
                                              Integer size,
                                              String sort,
                                              Integer expiringInDays,
                                              Long vendorId,
                                              String form,
                                              boolean summaryMode) {
        ValidationErrors errors = new ValidationErrors();

        String normalizedQ = normalizeQuery(q);
        if (normalizedQ != null && normalizedQ.length() < 2) {
            errors.add("q", "q must be at least 2 characters");
        }

        if (expiringInDays != null && expiringInDays < 0) {
            errors.add("expiringInDays", "expiringInDays must be greater than or equal to 0");
        }

        if (vendorId != null && vendorId <= 0) {
            errors.add("vendorId", "vendorId must be greater than 0");
        }

        String normalizedForm = normalizeForm(form, errors);

        SortInstruction sortInstruction = parseSort(sort, errors);
        errors.throwIfAny();

        return new QueryContext(
                normalizedQ,
                summaryMode ? DEFAULT_PAGE : (page == null ? DEFAULT_PAGE : page),
                summaryMode ? DEFAULT_SIZE : (size == null ? DEFAULT_SIZE : size),
                sortInstruction.field(),
                sortInstruction.direction(),
                expiringInDays,
                vendorId,
                normalizedForm
        );
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeForm(String form, ValidationErrors errors) {
        if (form == null || form.isBlank()) {
            return null;
        }

        String normalized = form.trim().toUpperCase(Locale.ROOT);
        try {
            return MedicineForm.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            errors.add("form", "form must be one of " + String.join(", ", enumNames(MedicineForm.values())));
            return null;
        }
    }

    private SortInstruction parseSort(String sort, ValidationErrors errors) {
        String candidate = (sort == null || sort.isBlank()) ? "medicineName,asc" : sort.trim();
        String[] parts = candidate.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            errors.add("sort", "unsupported sort field");
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
            errors.add("sort", "sort direction must be asc or desc");
        }

        return new SortInstruction(field, direction);
    }

    private ManageStockRowDto toManageStockRowDto(ManageStockRowProjection row, int expiryThreshold) {
        int availableQty = row.getAvailableQty() == null ? 0 : row.getAvailableQty();
        int reorderLevel = row.getReorderLevel() == null ? 0 : row.getReorderLevel();
        LocalDate nearestExpiryDate = row.getNearestExpiryDate();

        return ManageStockRowDto.builder()
                .medicineId(row.getMedicineId())
                .medicineCode(row.getMedicineCode())
                .medicineName(row.getMedicineName())
                .brand(row.getBrand())
                .form(row.getForm())
                .availableQty(availableQty)
                .reservedQty(row.getReservedQty() == null ? 0 : row.getReservedQty())
                .reorderLevel(reorderLevel)
                .sellingPrice(nullSafe(row.getSellingPrice()))
                .mrp(nullSafe(row.getMrp()))
                .nearestExpiryDate(nearestExpiryDate)
                .stockValue(nullSafe(row.getStockValue()))
                .stockStatus(resolveStockStatus(availableQty, reorderLevel, nearestExpiryDate, expiryThreshold))
                .lowStock(availableQty > 0 && availableQty <= reorderLevel)
                .inStock(availableQty > 0)
                .build();
    }

    private String resolveStockStatus(int availableQty, int reorderLevel, LocalDate nearestExpiryDate, int expiryThreshold) {
        if (availableQty <= 0) {
            return "OUT_OF_STOCK";
        }
        if (availableQty <= reorderLevel) {
            return "LOW_STOCK";
        }
        if (nearestExpiryDate != null && !nearestExpiryDate.isBefore(LocalDate.now())
                && !nearestExpiryDate.isAfter(LocalDate.now().plusDays(expiryThreshold))) {
            return "EXPIRING_SOON";
        }
        return "HEALTHY";
    }

    private MedicineStockBatchDto toMedicineStockBatchDto(MedicineBatchProjection row) {
        return MedicineStockBatchDto.builder()
                .batchId(row.getBatchId())
                .batchNo(row.getBatchNo())
                .vendorId(row.getVendorId())
                .vendorName(row.getVendorName())
                .expiryDate(row.getExpiryDate())
                .purchasePrice(nullSafe(row.getPurchasePrice()))
                .mrp(nullSafe(row.getMrp()))
                .sellingPrice(nullSafe(row.getSellingPrice()))
                .receivedQty(row.getReceivedQty() == null ? 0 : row.getReceivedQty())
                .availableQty(row.getAvailableQty() == null ? 0 : row.getAvailableQty())
                .expired(Boolean.TRUE.equals(row.getExpired()))
                .build();
    }

    private MedicineStockTransactionDto toMedicineStockTransactionDto(MedicineTransactionProjection row) {
        Instant transactionTime = row.getTransactionTime() == null
                ? null
                : row.getTransactionTime().toInstant(ZoneOffset.UTC);
        return MedicineStockTransactionDto.builder()
                .transactionId(row.getTransactionId())
                .transactionTime(transactionTime)
                .transactionType(row.getTransactionType())
                .batchNo(row.getBatchNo())
                .qtyIn(row.getQtyIn() == null ? 0 : row.getQtyIn())
                .qtyOut(row.getQtyOut() == null ? 0 : row.getQtyOut())
                .balanceAfter(row.getBalanceAfter() == null ? 0 : row.getBalanceAfter())
                .referenceType(row.getReferenceType())
                .referenceId(row.getReferenceId())
                .referenceNo(row.getReferenceNo())
                .note(row.getNote())
                .build();
    }

    private MdmMedicine getActiveMedicineOrThrow(Long medicineId) {
        MdmMedicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new MedicineNotFoundException(medicineId));
        if (Boolean.FALSE.equals(medicine.getIsActive())) {
            throw new MedicineNotFoundException(medicineId);
        }
        return medicine;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private List<String> enumNames(Enum<?>[] values) {
        return java.util.Arrays.stream(values).map(Enum::name).toList();
    }

    private record SortInstruction(String field, String direction) {
    }

    private record QueryContext(String q,
                                int page,
                                int size,
                                String sortField,
                                String sortDir,
                                Integer expiringInDays,
                                Long vendorId,
                                String form) {
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
