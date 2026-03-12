package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiptHistoryRowDto;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveRequest;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveRequestItem;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveResponseDto;
import com.MediHubAPI.enums.pharmacy.PurchaseOrderStatus;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.pharmacy.OverReceiptException;
import com.MediHubAPI.exception.pharmacy.PurchaseOrderItemNotFoundException;
import com.MediHubAPI.exception.pharmacy.PurchaseOrderNotFoundException;
import com.MediHubAPI.exception.pharmacy.PurchaseOrderReceiptNotAllowedException;
import com.MediHubAPI.model.pharmacy.*;
import com.MediHubAPI.repository.pharmacy.*;
import com.MediHubAPI.repository.projection.PurchaseOrderReceiptHistoryProjection;
import com.MediHubAPI.service.pharmacy.PharmacyPurchaseOrderReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PharmacyPurchaseOrderReceiptServiceImpl implements PharmacyPurchaseOrderReceiptService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final PharmacyPurchaseOrderRepository pharmacyPurchaseOrderRepository;
    private final PharmacyPurchaseOrderItemRepository pharmacyPurchaseOrderItemRepository;
    private final PharmacyStockBatchRepository pharmacyStockBatchRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PharmacyStockTransactionRepository pharmacyStockTransactionRepository;

    @Override
    @Transactional
    public PurchaseOrderReceiveResponseDto receive(Long purchaseOrderId, PurchaseOrderReceiveRequest request) {
        PharmacyPurchaseOrder purchaseOrder = pharmacyPurchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(purchaseOrderId));

        validatePurchaseOrderStatus(purchaseOrder);
        validateReceiptRequest(request);

        List<Long> itemIds = request.getItems().stream()
                .map(PurchaseOrderReceiveRequestItem::getPurchaseOrderItemId)
                .toList();

        List<PharmacyPurchaseOrderItem> purchaseOrderItems =
                pharmacyPurchaseOrderItemRepository.findAllForReceipt(purchaseOrderId, itemIds);

        if (purchaseOrderItems.size() != itemIds.size()) {
            Set<Long> foundIds = purchaseOrderItems.stream().map(PharmacyPurchaseOrderItem::getId).collect(Collectors.toSet());
            Long missingId = itemIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new PurchaseOrderItemNotFoundException(missingId);
        }

        Map<Long, PharmacyPurchaseOrderItem> itemMap = purchaseOrderItems.stream()
                .collect(Collectors.toMap(PharmacyPurchaseOrderItem::getId, Function.identity()));

        int totalReceivedQty = 0;
        LocalDateTime receivedAt = LocalDateTime.of(request.getReceiptDate(), LocalTime.now());

        for (PurchaseOrderReceiveRequestItem requestItem : request.getItems()) {
            PharmacyPurchaseOrderItem purchaseOrderItem = itemMap.get(requestItem.getPurchaseOrderItemId());
            int currentReceived = purchaseOrderItem.getReceivedQty() == null ? 0 : purchaseOrderItem.getReceivedQty();
            int orderedQty = purchaseOrderItem.getOrderedQty() == null ? 0 : purchaseOrderItem.getOrderedQty();
            int pendingQty = orderedQty - currentReceived;

            if (pendingQty <= 0 || requestItem.getReceivedQty() > pendingQty) {
                throw new OverReceiptException(purchaseOrderItem.getId(), requestItem.getReceivedQty(), Math.max(pendingQty, 0));
            }

            if (pharmacyStockBatchRepository.existsByMedicine_IdAndBatchNoIgnoreCaseAndExpiryDate(
                    purchaseOrderItem.getMedicine().getId(),
                    requestItem.getBatchNo().trim(),
                    requestItem.getExpiryDate()
            )) {
                throw new ValidationException("Validation failed", List.of(
                        new ValidationException.ValidationErrorDetail(
                                "items[" + request.getItems().indexOf(requestItem) + "].batchNo",
                                "batch already exists for the same medicine and expiry date"
                        )
                ));
            }

            PharmacyStockBatch batch = PharmacyStockBatch.builder()
                    .medicine(purchaseOrderItem.getMedicine())
                    .vendor(purchaseOrder.getVendor())
                    .batchNo(requestItem.getBatchNo().trim())
                    .expiryDate(requestItem.getExpiryDate())
                    .purchasePrice(requestItem.getPurchasePrice())
                    .mrp(requestItem.getMrp())
                    .sellingPrice(requestItem.getSellingPrice())
                    .receivedQty(requestItem.getReceivedQty())
                    .availableQty(requestItem.getReceivedQty())
                    .receivedAt(receivedAt)
                    .purchaseOrderItemId(purchaseOrderItem.getId())
                    .isActive(true)
                    .build();
            PharmacyStockBatch savedBatch = pharmacyStockBatchRepository.save(batch);

            PharmacyStock stock = pharmacyStockRepository.findByMedicine_Id(purchaseOrderItem.getMedicine().getId())
                    .orElseGet(() -> PharmacyStock.builder()
                            .medicine(purchaseOrderItem.getMedicine())
                            .availableQty(0)
                            .reservedQty(0)
                            .reorderLevel(0)
                            .build());

            int updatedAvailableQty = (stock.getAvailableQty() == null ? 0 : stock.getAvailableQty()) + requestItem.getReceivedQty();
            stock.setAvailableQty(updatedAvailableQty);
            pharmacyStockRepository.save(stock);

            PharmacyStockTransaction transaction = PharmacyStockTransaction.builder()
                    .medicine(purchaseOrderItem.getMedicine())
                    .batch(savedBatch)
                    .transactionType("PURCHASE_RECEIPT")
                    .qtyIn(requestItem.getReceivedQty())
                    .qtyOut(0)
                    .balanceAfter(updatedAvailableQty)
                    .unitCost(requestItem.getPurchasePrice())
                    .unitPrice(requestItem.getSellingPrice())
                    .referenceType("PURCHASE_ORDER")
                    .referenceId(purchaseOrder.getId())
                    .referenceNo(purchaseOrder.getPoNumber())
                    .note(composeNote(request.getNote(), requestItem.getNote()))
                    .createdBy(null)
                    .transactionTime(receivedAt)
                    .build();
            pharmacyStockTransactionRepository.save(transaction);

            purchaseOrderItem.setReceivedQty(currentReceived + requestItem.getReceivedQty());
            purchaseOrderItem.setPurchasePrice(requestItem.getPurchasePrice());
            purchaseOrderItem.setMrp(requestItem.getMrp());
            purchaseOrderItem.setSellingPrice(requestItem.getSellingPrice());

            totalReceivedQty += requestItem.getReceivedQty();
        }

        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().isBlank()) {
            purchaseOrder.setInvoiceNumber(request.getInvoiceNumber().trim());
        }
        if (request.getInvoiceDate() != null) {
            purchaseOrder.setInvoiceDate(request.getInvoiceDate());
        }

        long pendingItems = pharmacyPurchaseOrderItemRepository.countPendingItems(purchaseOrderId);
        purchaseOrder.setStatus(pendingItems == 0 ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);

        log.info("Received stock for purchaseOrderId={}, status={}, itemCount={}, qty={}",
                purchaseOrderId, purchaseOrder.getStatus(), request.getItems().size(), totalReceivedQty);

        return PurchaseOrderReceiveResponseDto.builder()
                .purchaseOrderId(purchaseOrderId)
                .status(purchaseOrder.getStatus().name())
                .receivedItemCount(request.getItems().size())
                .receivedQty(totalReceivedQty)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderReceiptHistoryRowDto> getReceiptHistory(Long purchaseOrderId, Integer page, Integer size) {
        if (!pharmacyPurchaseOrderRepository.existsById(purchaseOrderId)) {
            throw new PurchaseOrderNotFoundException(purchaseOrderId);
        }

        int safePage = page == null ? DEFAULT_PAGE : page;
        int safeSize = size == null ? DEFAULT_SIZE : size;

        Page<PurchaseOrderReceiptHistoryProjection> history = pharmacyStockBatchRepository.findReceiptHistoryByPurchaseOrderId(
                purchaseOrderId,
                PageRequest.of(safePage, safeSize)
        );

        return history.map(this::toReceiptHistoryRowDto);
    }

    private void validatePurchaseOrderStatus(PharmacyPurchaseOrder purchaseOrder) {
        PurchaseOrderStatus status = purchaseOrder.getStatus();
        if (status != PurchaseOrderStatus.APPROVED && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new PurchaseOrderReceiptNotAllowedException(purchaseOrder.getId(), status == null ? "UNKNOWN" : status.name());
        }
    }

    private void validateReceiptRequest(PurchaseOrderReceiveRequest request) {
        ValidationErrors errors = new ValidationErrors();

        if (request.getInvoiceDate() != null && request.getReceiptDate() != null
                && request.getInvoiceDate().isAfter(request.getReceiptDate())) {
            errors.add("invoiceDate", "invoiceDate cannot be after receiptDate");
        }

        Set<Long> seenItemIds = new HashSet<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            PurchaseOrderReceiveRequestItem item = request.getItems().get(i);
            if (!seenItemIds.add(item.getPurchaseOrderItemId())) {
                errors.add("items[" + i + "].purchaseOrderItemId", "duplicate purchaseOrderItemId in receipt request");
            }
            if (item.getExpiryDate() != null && request.getReceiptDate() != null && item.getExpiryDate().isBefore(request.getReceiptDate())) {
                errors.add("items[" + i + "].expiryDate", "expiryDate cannot be before receiptDate");
            }
        }

        errors.throwIfAny();
    }

    private String composeNote(String requestNote, String itemNote) {
        String left = requestNote == null ? "" : requestNote.trim();
        String right = itemNote == null ? "" : itemNote.trim();
        if (left.isEmpty()) {
            return right.isEmpty() ? null : right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + " | " + right;
    }

    private PurchaseOrderReceiptHistoryRowDto toReceiptHistoryRowDto(PurchaseOrderReceiptHistoryProjection row) {
        Instant receivedAt = row.getReceivedAt() == null ? null : row.getReceivedAt().toInstant(ZoneOffset.UTC);
        return PurchaseOrderReceiptHistoryRowDto.builder()
                .batchId(row.getBatchId())
                .purchaseOrderItemId(row.getPurchaseOrderItemId())
                .medicineId(row.getMedicineId())
                .medicineName(row.getMedicineName())
                .batchNo(row.getBatchNo())
                .expiryDate(row.getExpiryDate())
                .receivedQty(row.getReceivedQty())
                .purchasePrice(row.getPurchasePrice())
                .mrp(row.getMrp())
                .sellingPrice(row.getSellingPrice())
                .receivedAt(receivedAt)
                .build();
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
