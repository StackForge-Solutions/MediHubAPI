package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.CreateStockAdjustmentItemRequestDto;
import com.MediHubAPI.dto.pharmacy.CreateStockAdjustmentRequestDto;
import com.MediHubAPI.dto.pharmacy.CreateStockAdjustmentResponseDto;
import com.MediHubAPI.dto.pharmacy.StockAdjustmentDetailDto;
import com.MediHubAPI.dto.pharmacy.StockAdjustmentItemDto;
import com.MediHubAPI.dto.pharmacy.StockAdjustmentListRowDto;
import com.MediHubAPI.enums.pharmacy.StockAdjustmentReason;
import com.MediHubAPI.enums.pharmacy.StockAdjustmentStatus;
import com.MediHubAPI.enums.pharmacy.StockAdjustmentType;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.pharmacy.StockAdjustmentNotFoundException;
import com.MediHubAPI.model.mdm.MdmMedicine;
import com.MediHubAPI.model.pharmacy.PharmacyStock;
import com.MediHubAPI.model.pharmacy.PharmacyStockBatch;
import com.MediHubAPI.model.pharmacy.PharmacyStockTransaction;
import com.MediHubAPI.model.pharmacy.StockAdjustment;
import com.MediHubAPI.model.pharmacy.StockAdjustmentItem;
import com.MediHubAPI.repository.pharmacy.MdmMedicineRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockBatchRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockTransactionRepository;
import com.MediHubAPI.repository.pharmacy.StockAdjustmentItemRepository;
import com.MediHubAPI.repository.pharmacy.StockAdjustmentRepository;
import com.MediHubAPI.repository.projection.StockAdjustmentListRowProjection;
import com.MediHubAPI.service.pharmacy.PharmacyStockAdjustmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PharmacyStockAdjustmentServiceImpl implements PharmacyStockAdjustmentService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final DateTimeFormatter ADJUSTMENT_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String REFERENCE_TYPE = "STOCK_ADJUSTMENT";

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final StockAdjustmentItemRepository stockAdjustmentItemRepository;
    private final MdmMedicineRepository medicineRepository;
    private final PharmacyStockBatchRepository pharmacyStockBatchRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PharmacyStockTransactionRepository pharmacyStockTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StockAdjustmentListRowDto> getStockAdjustments(String q,
                                                               String reason,
                                                               String type,
                                                               LocalDate fromDate,
                                                               LocalDate toDate,
                                                               Integer page,
                                                               Integer size) {
        QueryContext ctx = validateListQuery(q, reason, type, fromDate, toDate, page, size);
        Page<StockAdjustmentListRowProjection> rows = stockAdjustmentRepository.searchAdjustments(
                ctx.q(),
                ctx.reason(),
                ctx.type(),
                ctx.fromDate(),
                ctx.toDate(),
                PageRequest.of(ctx.page(), ctx.size())
        );
        return rows.map(this::toListRowDto);
    }

    @Override
    @Transactional
    public CreateStockAdjustmentResponseDto createStockAdjustment(CreateStockAdjustmentRequestDto request) {
        ValidationErrors errors = new ValidationErrors();
        StockAdjustmentType adjustmentType = normalizeType(request.getAdjustmentType(), errors, "adjustmentType");
        StockAdjustmentReason adjustmentReason = normalizeReason(request.getReason(), errors, "reason");

        validateCreateRequest(request, adjustmentType, errors);
        Map<Long, MdmMedicine> medicines = loadMedicines(request.getItems(), errors);
        Map<Long, PharmacyStockBatch> batches = loadBatchesForUpdate(request.getItems(), errors);
        Map<Long, PharmacyStock> stocks = loadStocksForUpdate(request.getItems());
        validatePostingState(request, adjustmentType, medicines, batches, stocks, errors);
        errors.throwIfAny();

        String actor = currentActor();
        LocalDateTime transactionTime = LocalDateTime.of(request.getAdjustmentDate(), LocalTime.now());

        StockAdjustment adjustment = StockAdjustment.builder()
                .adjustmentNo("PENDING")
                .adjustmentDate(request.getAdjustmentDate())
                .adjustmentType(adjustmentType)
                .reason(adjustmentReason)
                .note(trimToNull(request.getNote()))
                .status(StockAdjustmentStatus.POSTED)
                .createdBy(actor)
                .build();
        stockAdjustmentRepository.save(adjustment);

        adjustment.setAdjustmentNo(generateAdjustmentNo(adjustment.getAdjustmentDate(), adjustment.getId()));
        stockAdjustmentRepository.save(adjustment);

        List<StockAdjustmentItem> adjustmentItems = new ArrayList<>();
        List<PharmacyStockTransaction> transactions = new ArrayList<>();
        Set<Long> touchedMedicineIds = new LinkedHashSet<>();

        for (CreateStockAdjustmentItemRequestDto itemRequest : request.getItems()) {
            MdmMedicine medicine = medicines.get(itemRequest.getMedicineId());
            PharmacyStockBatch batch = batches.get(itemRequest.getBatchId());
            PharmacyStock stock = stocks.computeIfAbsent(medicine.getId(), ignored -> PharmacyStock.builder()
                    .medicine(medicine)
                    .availableQty(0)
                    .reservedQty(0)
                    .reorderLevel(0)
                    .build());

            int currentStockQty = nullSafe(stock.getAvailableQty());
            int currentBatchQty = nullSafe(batch.getAvailableQty());
            int qty = itemRequest.getQty();
            int newStockQty;

            if (adjustmentType == StockAdjustmentType.DECREASE) {
                newStockQty = currentStockQty - qty;
                batch.setAvailableQty(currentBatchQty - qty);
            } else {
                newStockQty = currentStockQty + qty;
                batch.setAvailableQty(currentBatchQty + qty);
                batch.setReceivedQty(nullSafe(batch.getReceivedQty()) + qty);
            }

            stock.setAvailableQty(newStockQty);
            touchedMedicineIds.add(medicine.getId());

            BigDecimal unitCost = scale(itemRequest.getUnitCost() == null ? batch.getPurchasePrice() : itemRequest.getUnitCost());
            BigDecimal lineValue = unitCost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            adjustmentItems.add(StockAdjustmentItem.builder()
                    .stockAdjustment(adjustment)
                    .medicine(medicine)
                    .batch(batch)
                    .qty(qty)
                    .unitCost(unitCost)
                    .lineValue(lineValue)
                    .note(trimToNull(itemRequest.getNote()))
                    .build());

            transactions.add(PharmacyStockTransaction.builder()
                    .medicine(medicine)
                    .batch(batch)
                    .transactionType(adjustmentType == StockAdjustmentType.INCREASE ? "ADJUSTMENT_IN" : "ADJUSTMENT_OUT")
                    .qtyIn(adjustmentType == StockAdjustmentType.INCREASE ? qty : 0)
                    .qtyOut(adjustmentType == StockAdjustmentType.DECREASE ? qty : 0)
                    .balanceAfter(newStockQty)
                    .unitCost(unitCost)
                    .unitPrice(batch.getSellingPrice())
                    .referenceType(REFERENCE_TYPE)
                    .referenceId(adjustment.getId())
                    .referenceNo(adjustment.getAdjustmentNo())
                    .note(composeNote(adjustment.getNote(), itemRequest.getNote()))
                    .createdBy(actor)
                    .transactionTime(transactionTime)
                    .build());
        }

        stockAdjustmentItemRepository.saveAll(adjustmentItems);
        pharmacyStockBatchRepository.saveAll(new ArrayList<>(batches.values()));
        pharmacyStockRepository.saveAll(stocks.values().stream()
                .filter(stock -> touchedMedicineIds.contains(stock.getMedicine().getId()))
                .toList());
        pharmacyStockTransactionRepository.saveAll(transactions);

        log.info("Posted stock adjustment id={}, no={}, type={}, itemCount={}",
                adjustment.getId(), adjustment.getAdjustmentNo(), adjustment.getAdjustmentType(), adjustmentItems.size());

        return CreateStockAdjustmentResponseDto.builder()
                .adjustmentId(adjustment.getId())
                .adjustmentNo(adjustment.getAdjustmentNo())
                .status(adjustment.getStatus().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentDetailDto getStockAdjustment(Long adjustmentId) {
        StockAdjustment adjustment = stockAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new StockAdjustmentNotFoundException(adjustmentId));
        List<StockAdjustmentItem> items = stockAdjustmentItemRepository.findByStockAdjustmentIdWithRelations(adjustmentId);

        return StockAdjustmentDetailDto.builder()
                .adjustmentId(adjustment.getId())
                .adjustmentNo(adjustment.getAdjustmentNo())
                .adjustmentDate(adjustment.getAdjustmentDate())
                .adjustmentType(adjustment.getAdjustmentType().name())
                .reason(adjustment.getReason().name())
                .note(adjustment.getNote())
                .createdBy(adjustment.getCreatedBy())
                .status(adjustment.getStatus().name())
                .items(items.stream().map(this::toItemDto).toList())
                .build();
    }

    private QueryContext validateListQuery(String q,
                                          String reason,
                                          String type,
                                          LocalDate fromDate,
                                          LocalDate toDate,
                                          Integer page,
                                          Integer size) {
        ValidationErrors errors = new ValidationErrors();
        String normalizedQ = normalizeQuery(q);

        if (normalizedQ != null && normalizedQ.length() < 2) {
            errors.add("q", "q must be at least 2 characters");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            errors.add("fromDate", "fromDate must be before or equal to toDate");
        }

        StockAdjustmentReason normalizedReason = normalizeReason(reason, errors, "reason");
        StockAdjustmentType normalizedType = normalizeType(type, errors, "type");
        errors.throwIfAny();

        return new QueryContext(
                normalizedQ,
                normalizedReason == null ? null : normalizedReason.name(),
                normalizedType == null ? null : normalizedType.name(),
                fromDate,
                toDate,
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size
        );
    }

    private void validateCreateRequest(CreateStockAdjustmentRequestDto request,
                                       StockAdjustmentType adjustmentType,
                                       ValidationErrors errors) {
        if (adjustmentType == null) {
            errors.add("adjustmentType", "adjustmentType is required");
        }
        if (request.getAdjustmentDate() == null) {
            errors.add("adjustmentDate", "adjustmentDate is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            errors.add("items", "items must not be empty");
            return;
        }

        if (adjustmentType == StockAdjustmentType.DECREASE && trimToNull(request.getNote()) == null) {
            errors.add("note", "note is required for DECREASE adjustments");
        }

        Set<String> seenKeys = new HashSet<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            CreateStockAdjustmentItemRequestDto item = request.getItems().get(i);
            String key = item.getMedicineId() + ":" + item.getBatchId();
            if (!seenKeys.add(key)) {
                errors.add("items[" + i + "]", "duplicate medicineId and batchId combination is not allowed");
            }
        }
    }

    private Map<Long, MdmMedicine> loadMedicines(List<CreateStockAdjustmentItemRequestDto> items, ValidationErrors errors) {
        Set<Long> medicineIds = items.stream()
                .map(CreateStockAdjustmentItemRequestDto::getMedicineId)
                .collect(Collectors.toCollection(TreeSet::new));

        Map<Long, MdmMedicine> medicines = medicineRepository.findAllById(medicineIds).stream()
                .collect(Collectors.toMap(MdmMedicine::getId, Function.identity()));

        for (int i = 0; i < items.size(); i++) {
            Long medicineId = items.get(i).getMedicineId();
            MdmMedicine medicine = medicines.get(medicineId);
            if (medicine == null || Boolean.FALSE.equals(medicine.getIsActive())) {
                errors.add("items[" + i + "].medicineId", "medicine not found: " + medicineId);
            }
        }

        return medicines;
    }

    private Map<Long, PharmacyStockBatch> loadBatchesForUpdate(List<CreateStockAdjustmentItemRequestDto> items, ValidationErrors errors) {
        Set<Long> batchIds = items.stream()
                .map(CreateStockAdjustmentItemRequestDto::getBatchId)
                .collect(Collectors.toCollection(TreeSet::new));
        if (batchIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, PharmacyStockBatch> batches = pharmacyStockBatchRepository.findAllByIdForUpdate(batchIds).stream()
                .collect(Collectors.toMap(PharmacyStockBatch::getId, Function.identity()));

        for (int i = 0; i < items.size(); i++) {
            Long batchId = items.get(i).getBatchId();
            if (!batches.containsKey(batchId)) {
                errors.add("items[" + i + "].batchId", "batch not found: " + batchId);
            }
        }

        return batches;
    }

    private Map<Long, PharmacyStock> loadStocksForUpdate(List<CreateStockAdjustmentItemRequestDto> items) {
        Set<Long> medicineIds = items.stream()
                .map(CreateStockAdjustmentItemRequestDto::getMedicineId)
                .collect(Collectors.toCollection(TreeSet::new));
        if (medicineIds.isEmpty()) {
            return new HashMap<>();
        }

        return pharmacyStockRepository.findAllByMedicineIdsForUpdate(medicineIds).stream()
                .collect(Collectors.toMap(stock -> stock.getMedicine().getId(), Function.identity(), (left, right) -> left, HashMap::new));
    }

    private void validatePostingState(CreateStockAdjustmentRequestDto request,
                                      StockAdjustmentType adjustmentType,
                                      Map<Long, MdmMedicine> medicines,
                                      Map<Long, PharmacyStockBatch> batches,
                                      Map<Long, PharmacyStock> stocks,
                                      ValidationErrors errors) {
        if (adjustmentType == null) {
            return;
        }

        Map<Long, Integer> remainingStockByMedicine = new HashMap<>();
        Map<Long, Integer> remainingBatchById = new HashMap<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            CreateStockAdjustmentItemRequestDto item = request.getItems().get(i);
            MdmMedicine medicine = medicines.get(item.getMedicineId());
            PharmacyStockBatch batch = batches.get(item.getBatchId());

            if (medicine == null || batch == null) {
                continue;
            }

            if (batch.getMedicine() == null || !Objects.equals(batch.getMedicine().getId(), medicine.getId())) {
                errors.add("items[" + i + "].batchId", "batch does not belong to the selected medicine");
                continue;
            }

            if (Boolean.FALSE.equals(batch.getIsActive())) {
                errors.add("items[" + i + "].batchId", "batch is inactive");
            }

            int requestedQty = item.getQty();
            int batchAvailableQty = remainingBatchById.computeIfAbsent(batch.getId(), ignored -> nullSafe(batch.getAvailableQty()));
            PharmacyStock stock = stocks.get(medicine.getId());
            int stockAvailableQty = remainingStockByMedicine.computeIfAbsent(
                    medicine.getId(),
                    ignored -> stock == null ? 0 : nullSafe(stock.getAvailableQty())
            );

            if (adjustmentType == StockAdjustmentType.DECREASE) {
                if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(request.getAdjustmentDate())) {
                    errors.add("items[" + i + "].batchId", "expired batches cannot be used for DECREASE adjustments");
                }
                if (requestedQty > batchAvailableQty) {
                    errors.add("items[" + i + "].qty", "qty exceeds available batch stock");
                }
                if (requestedQty > stockAvailableQty) {
                    errors.add("items[" + i + "].qty", "qty exceeds current stock");
                }
                remainingBatchById.put(batch.getId(), batchAvailableQty - requestedQty);
                remainingStockByMedicine.put(medicine.getId(), stockAvailableQty - requestedQty);
            } else {
                remainingBatchById.put(batch.getId(), batchAvailableQty + requestedQty);
                remainingStockByMedicine.put(medicine.getId(), stockAvailableQty + requestedQty);
            }
        }
    }

    private StockAdjustmentListRowDto toListRowDto(StockAdjustmentListRowProjection row) {
        return StockAdjustmentListRowDto.builder()
                .adjustmentId(row.getAdjustmentId())
                .adjustmentNo(row.getAdjustmentNo())
                .adjustmentDate(row.getAdjustmentDate())
                .adjustmentType(row.getAdjustmentType())
                .reason(row.getReason())
                .medicineCount(row.getMedicineCount() == null ? 0 : row.getMedicineCount().intValue())
                .totalQtyImpact(row.getTotalQtyImpact() == null ? 0 : row.getTotalQtyImpact().intValue())
                .createdBy(row.getCreatedBy())
                .status(row.getStatus())
                .build();
    }

    private StockAdjustmentItemDto toItemDto(StockAdjustmentItem item) {
        return StockAdjustmentItemDto.builder()
                .medicineId(item.getMedicine().getId())
                .medicineName(item.getMedicine().getBrand())
                .batchId(item.getBatch().getId())
                .batchNo(item.getBatch().getBatchNo())
                .qty(item.getQty())
                .unitCost(scale(item.getUnitCost()))
                .lineValue(scale(item.getLineValue()))
                .note(item.getNote())
                .build();
    }

    private StockAdjustmentType normalizeType(String value, ValidationErrors errors, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return StockAdjustmentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            errors.add(field, field + " must be one of " + String.join(", ", enumNames(StockAdjustmentType.values())));
            return null;
        }
    }

    private StockAdjustmentReason normalizeReason(String value, ValidationErrors errors, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return StockAdjustmentReason.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            errors.add(field, field + " must be one of " + String.join(", ", enumNames(StockAdjustmentReason.values())));
            return null;
        }
    }

    private List<String> enumNames(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateAdjustmentNo(LocalDate adjustmentDate, Long id) {
        return "ADJ-" + adjustmentDate.format(ADJUSTMENT_DATE_FORMAT) + "-" + String.format("%06d", id);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String actor = authentication.getName();
        if (actor == null || actor.isBlank() || "anonymousUser".equalsIgnoreCase(actor)) {
            return "system";
        }
        return actor;
    }

    private String composeNote(String headerNote, String itemNote) {
        String left = trimToNull(headerNote);
        String right = trimToNull(itemNote);
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left + " | " + right;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private record QueryContext(String q,
                                String reason,
                                String type,
                                LocalDate fromDate,
                                LocalDate toDate,
                                int page,
                                int size) {
    }

    private static class ValidationErrors {
        private final ArrayList<ValidationException.ValidationErrorDetail> details = new ArrayList<>();

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
