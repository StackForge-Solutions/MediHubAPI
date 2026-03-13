package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.*;
import com.MediHubAPI.enums.pharmacy.PurchaseOrderStatus;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.pharmacy.PharmacyVendorNotFoundException;
import com.MediHubAPI.exception.pharmacy.PurchaseOrderNotFoundException;
import com.MediHubAPI.exception.pharmacy.PurchaseOrderStateException;
import com.MediHubAPI.model.mdm.MdmMedicine;
import com.MediHubAPI.model.pharmacy.PharmacyPurchaseOrder;
import com.MediHubAPI.model.pharmacy.PharmacyPurchaseOrderItem;
import com.MediHubAPI.model.pharmacy.PharmacyVendor;
import com.MediHubAPI.repository.pharmacy.MdmMedicineRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyPurchaseOrderItemRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyPurchaseOrderRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyVendorRepository;
import com.MediHubAPI.repository.projection.PurchaseOrderRowProjection;
import com.MediHubAPI.service.pharmacy.PharmacyPurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PharmacyPurchaseOrderServiceImpl implements PharmacyPurchaseOrderService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("poNumber", "vendorName", "orderDate", "status", "netAmount");
    private static final DateTimeFormatter PO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final PharmacyPurchaseOrderRepository purchaseOrderRepository;
    private final PharmacyPurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PharmacyVendorRepository vendorRepository;
    private final MdmMedicineRepository medicineRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderRowDto> getPurchaseOrders(String q,
                                                       String status,
                                                       Long vendorId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate,
                                                       Integer page,
                                                       Integer size,
                                                       String sort) {
        QueryContext ctx = validateListQuery(q, status, vendorId, fromDate, toDate, page, size, sort);
        Page<PurchaseOrderRowProjection> result = purchaseOrderRepository.searchPurchaseOrders(
                ctx.q(),
                ctx.status(),
                ctx.vendorId(),
                ctx.fromDate(),
                ctx.toDate(),
                ctx.sortField(),
                ctx.sortDir(),
                PageRequest.of(ctx.page(), ctx.size())
        );

        return result.map(row -> PurchaseOrderRowDto.builder()
                .purchaseOrderId(row.getPurchaseOrderId())
                .poNumber(row.getPoNumber())
                .vendorId(row.getVendorId())
                .vendorName(row.getVendorName())
                .orderDate(row.getOrderDate())
                .status(row.getStatus())
                .itemCount(row.getItemCount())
                .orderedQty(row.getOrderedQty() == null ? 0 : row.getOrderedQty())
                .receivedQty(row.getReceivedQty() == null ? 0 : row.getReceivedQty())
                .netAmount(nullSafe(row.getNetAmount()))
                .build());
    }

    @Override
    @Transactional
    public PurchaseOrderDetailDto createPurchaseOrder(PurchaseOrderCreateRequest request) {
        ValidationErrors errors = new ValidationErrors();
        validateUpsertRequest(request, errors);
        errors.throwIfAny();

        PharmacyVendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new PharmacyVendorNotFoundException(request.getVendorId()));
        Map<Long, MdmMedicine> medicines = loadMedicines(request.getItems(), errors);
        errors.throwIfAny();

        PharmacyPurchaseOrder purchaseOrder = PharmacyPurchaseOrder.builder()
                .vendor(vendor)
                .poNumber(generatePoNumber())
                .orderDate(request.getOrderDate())
                .invoiceNumber(trimToNull(request.getInvoiceNumber()))
                .invoiceDate(request.getInvoiceDate())
                .status(PurchaseOrderStatus.DRAFT)
                .note(trimToNull(request.getNote()))
                .build();
        purchaseOrderRepository.save(purchaseOrder);

        persistItemsAndTotals(purchaseOrder, request.getItems(), medicines);
        purchaseOrderRepository.save(purchaseOrder);

        log.info("Created purchase order id={}, poNumber={}", purchaseOrder.getId(), purchaseOrder.getPoNumber());
        return getPurchaseOrder(purchaseOrder.getId());
    }

    @Override
    @Transactional
    public PurchaseOrderDetailDto updatePurchaseOrder(Long purchaseOrderId, PurchaseOrderUpdateRequest request) {
        ValidationErrors errors = new ValidationErrors();
        validateUpsertRequest(request, errors);
        errors.throwIfAny();

        PharmacyPurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdWithVendor(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(purchaseOrderId));
        ensureDraft(purchaseOrder, "PURCHASE_ORDER_UPDATE_NOT_ALLOWED", "Only DRAFT purchase orders can be updated");

        PharmacyVendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new PharmacyVendorNotFoundException(request.getVendorId()));
        Map<Long, MdmMedicine> medicines = loadMedicines(request.getItems(), errors);
        errors.throwIfAny();

        purchaseOrder.setVendor(vendor);
        purchaseOrder.setOrderDate(request.getOrderDate());
        purchaseOrder.setInvoiceNumber(trimToNull(request.getInvoiceNumber()));
        purchaseOrder.setInvoiceDate(request.getInvoiceDate());
        purchaseOrder.setNote(trimToNull(request.getNote()));

        purchaseOrderItemRepository.deleteByPurchaseOrder_Id(purchaseOrderId);
        persistItemsAndTotals(purchaseOrder, request.getItems(), medicines);
        purchaseOrderRepository.save(purchaseOrder);

        log.info("Updated purchase order id={}, poNumber={}", purchaseOrder.getId(), purchaseOrder.getPoNumber());
        return getPurchaseOrder(purchaseOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDetailDto getPurchaseOrder(Long purchaseOrderId) {
        PharmacyPurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdWithVendor(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(purchaseOrderId));
        List<PharmacyPurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderIdWithMedicine(purchaseOrderId);

        return PurchaseOrderDetailDto.builder()
                .purchaseOrderId(purchaseOrder.getId())
                .poNumber(purchaseOrder.getPoNumber())
                .vendorId(purchaseOrder.getVendor().getId())
                .vendorName(purchaseOrder.getVendor().getVendorName())
                .orderDate(purchaseOrder.getOrderDate())
                .invoiceNumber(purchaseOrder.getInvoiceNumber())
                .invoiceDate(purchaseOrder.getInvoiceDate())
                .status(purchaseOrder.getStatus().name())
                .subtotal(nullSafe(purchaseOrder.getSubtotal()))
                .taxAmount(nullSafe(purchaseOrder.getTaxAmount()))
                .discountAmount(nullSafe(purchaseOrder.getDiscountAmount()))
                .netAmount(nullSafe(purchaseOrder.getNetAmount()))
                .note(purchaseOrder.getNote())
                .items(items.stream().map(this::toItemDto).toList())
                .build();
    }

    @Override
    @Transactional
    public PurchaseOrderActionResponseDto approvePurchaseOrder(Long purchaseOrderId) {
        PharmacyPurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(purchaseOrderId));
        ensureDraft(purchaseOrder, "PURCHASE_ORDER_APPROVE_NOT_ALLOWED", "Only DRAFT purchase orders can be approved");
        if (purchaseOrderItemRepository.findByPurchaseOrderIdWithMedicine(purchaseOrderId).isEmpty()) {
            throw new PurchaseOrderStateException("PURCHASE_ORDER_APPROVE_NOT_ALLOWED", "Purchase order must contain at least one item");
        }
        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrderRepository.save(purchaseOrder);
        return toActionResponse(purchaseOrder);
    }

    @Override
    @Transactional
    public PurchaseOrderActionResponseDto cancelPurchaseOrder(Long purchaseOrderId) {
        PharmacyPurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(purchaseOrderId));
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new PurchaseOrderStateException("PURCHASE_ORDER_CANCEL_NOT_ALLOWED", "RECEIVED purchase orders cannot be cancelled");
        }
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new PurchaseOrderStateException("PURCHASE_ORDER_CANCEL_NOT_ALLOWED", "Purchase order is already CANCELLED");
        }
        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(purchaseOrder);
        return toActionResponse(purchaseOrder);
    }

    private void persistItemsAndTotals(PharmacyPurchaseOrder purchaseOrder,
                                       List<PurchaseOrderItemRequest> requests,
                                       Map<Long, MdmMedicine> medicines) {
        Totals totals = calculateTotals(requests);
        purchaseOrder.setSubtotal(totals.subtotal());
        purchaseOrder.setTaxAmount(totals.taxAmount());
        purchaseOrder.setDiscountAmount(totals.discountAmount());
        purchaseOrder.setNetAmount(totals.netAmount());

        List<PharmacyPurchaseOrderItem> items = new ArrayList<>();
        for (PurchaseOrderItemRequest req : requests) {
            BigDecimal taxPercent = scale(req.getTaxPercent());
            BigDecimal discountPercent = scale(req.getDiscountPercent());
            BigDecimal lineBase = nullSafe(req.getPurchasePrice()).multiply(BigDecimal.valueOf(req.getOrderedQty()));
            BigDecimal lineTax = percentage(lineBase, taxPercent);
            BigDecimal lineDiscount = percentage(lineBase, discountPercent);
            BigDecimal lineTotal = lineBase.add(lineTax).subtract(lineDiscount);

            items.add(PharmacyPurchaseOrderItem.builder()
                    .purchaseOrder(purchaseOrder)
                    .medicine(medicines.get(req.getMedicineId()))
                    .orderedQty(req.getOrderedQty())
                    .receivedQty(0)
                    .purchasePrice(scale(req.getPurchasePrice()))
                    .mrp(scale(req.getMrp()))
                    .sellingPrice(scale(req.getSellingPrice()))
                    .taxPercent(taxPercent)
                    .discountPercent(discountPercent)
                    .lineTotal(scale(lineTotal))
                    .build());
        }
        purchaseOrderItemRepository.saveAll(items);
    }

    private Totals calculateTotals(List<PurchaseOrderItemRequest> requests) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemRequest req : requests) {
            BigDecimal base = nullSafe(req.getPurchasePrice()).multiply(BigDecimal.valueOf(req.getOrderedQty()));
            BigDecimal tax = percentage(base, req.getTaxPercent());
            BigDecimal discount = percentage(base, req.getDiscountPercent());
            subtotal = subtotal.add(base);
            taxAmount = taxAmount.add(tax);
            discountAmount = discountAmount.add(discount);
        }

        BigDecimal netAmount = subtotal.add(taxAmount).subtract(discountAmount);
        return new Totals(scale(subtotal), scale(taxAmount), scale(discountAmount), scale(netAmount));
    }

    private Map<Long, MdmMedicine> loadMedicines(List<PurchaseOrderItemRequest> items, ValidationErrors errors) {
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            Long medicineId = items.get(i).getMedicineId();
            if (!seen.add(medicineId)) {
                errors.add("items[" + i + "].medicineId", "duplicate medicineId in purchase order");
            }
        }

        List<Long> ids = items.stream().map(PurchaseOrderItemRequest::getMedicineId).distinct().toList();
        Map<Long, MdmMedicine> medicines = medicineRepository.findAllById(ids).stream()
                .filter(m -> !Boolean.FALSE.equals(m.getIsActive()))
                .collect(Collectors.toMap(MdmMedicine::getId, m -> m));

        for (int i = 0; i < items.size(); i++) {
            Long medicineId = items.get(i).getMedicineId();
            if (!medicines.containsKey(medicineId)) {
                errors.add("items[" + i + "].medicineId", "medicine not found: " + medicineId);
            }
        }
        return medicines;
    }

    private void validateUpsertRequest(PurchaseOrderCreateRequest request, ValidationErrors errors) {
        for (int i = 0; i < request.getItems().size(); i++) {
            PurchaseOrderItemRequest item = request.getItems().get(i);
            if (nullSafe(item.getTaxPercent()).compareTo(BigDecimal.valueOf(100)) > 0) {
                errors.add("items[" + i + "].taxPercent", "taxPercent must be less than or equal to 100");
            }
            if (nullSafe(item.getDiscountPercent()).compareTo(BigDecimal.valueOf(100)) > 0) {
                errors.add("items[" + i + "].discountPercent", "discountPercent must be less than or equal to 100");
            }
        }
    }

    private QueryContext validateListQuery(String q,
                                           String status,
                                           Long vendorId,
                                           LocalDate fromDate,
                                           LocalDate toDate,
                                           Integer page,
                                           Integer size,
                                           String sort) {
        ValidationErrors errors = new ValidationErrors();
        String normalizedQ = trimToNull(q);
        if (normalizedQ != null && normalizedQ.length() < 2) {
            errors.add("q", "q must be at least 2 characters");
        }
        if (vendorId != null && vendorId <= 0) {
            errors.add("vendorId", "vendorId must be greater than 0");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            errors.add("fromDate", "fromDate must be less than or equal to toDate");
        }

        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                normalizedStatus = PurchaseOrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
            } catch (IllegalArgumentException ex) {
                errors.add("status", "status must be one of " + String.join(", ", Arrays.stream(PurchaseOrderStatus.values()).map(Enum::name).toList()));
            }
        }

        String candidate = (sort == null || sort.isBlank()) ? "orderDate,desc" : sort.trim();
        String[] parts = candidate.split(",");
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            errors.add("sort", "unsupported sort field");
        }
        if (!dir.equals("asc") && !dir.equals("desc")) {
            errors.add("sort", "sort direction must be asc or desc");
        }

        errors.throwIfAny();
        return new QueryContext(normalizedQ, normalizedStatus, vendorId, fromDate, toDate,
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size,
                field, dir);
    }

    private void ensureDraft(PharmacyPurchaseOrder purchaseOrder, String code, String message) {
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new PurchaseOrderStateException(code, message);
        }
    }

    private PurchaseOrderItemDto toItemDto(PharmacyPurchaseOrderItem item) {
        int orderedQty = item.getOrderedQty() == null ? 0 : item.getOrderedQty();
        int receivedQty = item.getReceivedQty() == null ? 0 : item.getReceivedQty();
        return PurchaseOrderItemDto.builder()
                .itemId(item.getId())
                .medicineId(item.getMedicine().getId())
                .medicineName(item.getMedicine().getBrand())
                .orderedQty(orderedQty)
                .receivedQty(receivedQty)
                .pendingQty(Math.max(orderedQty - receivedQty, 0))
                .purchasePrice(scale(item.getPurchasePrice()))
                .mrp(scale(item.getMrp()))
                .sellingPrice(scale(item.getSellingPrice()))
                .taxPercent(scale(item.getTaxPercent()))
                .discountPercent(scale(item.getDiscountPercent()))
                .lineTotal(scale(item.getLineTotal()))
                .build();
    }

    private PurchaseOrderActionResponseDto toActionResponse(PharmacyPurchaseOrder purchaseOrder) {
        return PurchaseOrderActionResponseDto.builder()
                .purchaseOrderId(purchaseOrder.getId())
                .poNumber(purchaseOrder.getPoNumber())
                .status(purchaseOrder.getStatus().name())
                .build();
    }

    private String generatePoNumber() {
        return "PO-" + LocalDate.now().format(PO_DATE_FORMAT) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private BigDecimal percentage(BigDecimal base, BigDecimal percent) {
        return nullSafe(base).multiply(nullSafe(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return nullSafe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record QueryContext(String q, String status, Long vendorId, LocalDate fromDate, LocalDate toDate,
                                int page, int size, String sortField, String sortDir) {
    }

    private record Totals(BigDecimal subtotal, BigDecimal taxAmount, BigDecimal discountAmount, BigDecimal netAmount) {
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
