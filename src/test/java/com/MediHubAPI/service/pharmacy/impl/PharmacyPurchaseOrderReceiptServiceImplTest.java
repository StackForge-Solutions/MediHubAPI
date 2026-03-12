package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiptHistoryRowDto;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveRequest;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveRequestItem;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveResponseDto;
import com.MediHubAPI.enums.pharmacy.PurchaseOrderStatus;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.pharmacy.PurchaseOrderNotFoundException;
import com.MediHubAPI.exception.pharmacy.PurchaseOrderReceiptNotAllowedException;
import com.MediHubAPI.model.mdm.MdmMedicine;
import com.MediHubAPI.model.pharmacy.PharmacyPurchaseOrder;
import com.MediHubAPI.model.pharmacy.PharmacyPurchaseOrderItem;
import com.MediHubAPI.model.pharmacy.PharmacyStock;
import com.MediHubAPI.model.pharmacy.PharmacyStockBatch;
import com.MediHubAPI.model.pharmacy.PharmacyStockTransaction;
import com.MediHubAPI.model.pharmacy.PharmacyVendor;
import com.MediHubAPI.repository.pharmacy.PharmacyPurchaseOrderItemRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyPurchaseOrderRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockBatchRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockRepository;
import com.MediHubAPI.repository.pharmacy.PharmacyStockTransactionRepository;
import com.MediHubAPI.repository.projection.PurchaseOrderReceiptHistoryProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyPurchaseOrderReceiptServiceImplTest {

    @Mock
    private PharmacyPurchaseOrderRepository pharmacyPurchaseOrderRepository;

    @Mock
    private PharmacyPurchaseOrderItemRepository pharmacyPurchaseOrderItemRepository;

    @Mock
    private PharmacyStockBatchRepository pharmacyStockBatchRepository;

    @Mock
    private PharmacyStockRepository pharmacyStockRepository;

    @Mock
    private PharmacyStockTransactionRepository pharmacyStockTransactionRepository;

    @InjectMocks
    private PharmacyPurchaseOrderReceiptServiceImpl service;

    @Test
    @DisplayName("Receive posts batch, stock summary, ledger, and PO status")
    void receivePostsInventoryAndMarksPurchaseOrderReceived() {
        PharmacyVendor vendor = vendor();
        MdmMedicine medicine = medicine(101L);
        PharmacyPurchaseOrder purchaseOrder = purchaseOrder(vendor, PurchaseOrderStatus.APPROVED);
        PharmacyPurchaseOrderItem purchaseOrderItem = purchaseOrderItem(purchaseOrder, medicine, 100, 20);
        PharmacyStock stock = PharmacyStock.builder()
                .id(700L)
                .medicine(medicine)
                .availableQty(15)
                .reservedQty(0)
                .reorderLevel(5)
                .build();

        PurchaseOrderReceiveRequest request = PurchaseOrderReceiveRequest.builder()
                .receiptDate(LocalDate.of(2026, 3, 12))
                .invoiceNumber(" INV-889 ")
                .invoiceDate(LocalDate.of(2026, 3, 12))
                .note("Weekly vendor delivery")
                .items(List.of(PurchaseOrderReceiveRequestItem.builder()
                        .purchaseOrderItemId(801L)
                        .batchNo(" BATCH-APR-01 ")
                        .expiryDate(LocalDate.of(2026, 8, 31))
                        .receivedQty(30)
                        .purchasePrice(new BigDecimal("1.75"))
                        .mrp(new BigDecimal("3.00"))
                        .sellingPrice(new BigDecimal("2.50"))
                        .note("Shelf A")
                        .build()))
                .build();

        when(pharmacyPurchaseOrderRepository.findByIdForUpdate(501L)).thenReturn(Optional.of(purchaseOrder));
        when(pharmacyPurchaseOrderItemRepository.findAllForReceipt(eq(501L), anyCollection()))
                .thenReturn(List.of(purchaseOrderItem));
        when(pharmacyStockBatchRepository.existsByMedicine_IdAndBatchNoIgnoreCaseAndExpiryDate(
                101L, "BATCH-APR-01", LocalDate.of(2026, 8, 31)
        )).thenReturn(false);
        when(pharmacyStockBatchRepository.save(any(PharmacyStockBatch.class))).thenAnswer(invocation -> {
            PharmacyStockBatch batch = invocation.getArgument(0);
            batch.setId(9001L);
            return batch;
        });
        when(pharmacyStockRepository.findByMedicine_Id(101L)).thenReturn(Optional.of(stock));
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pharmacyStockTransactionRepository.save(any(PharmacyStockTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pharmacyPurchaseOrderItemRepository.countPendingItems(501L)).thenReturn(0L);

        PurchaseOrderReceiveResponseDto response = service.receive(501L, request);

        assertThat(response.getPurchaseOrderId()).isEqualTo(501L);
        assertThat(response.getStatus()).isEqualTo("RECEIVED");
        assertThat(response.getReceivedItemCount()).isEqualTo(1);
        assertThat(response.getReceivedQty()).isEqualTo(30);

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(purchaseOrder.getInvoiceNumber()).isEqualTo("INV-889");
        assertThat(purchaseOrder.getInvoiceDate()).isEqualTo(LocalDate.of(2026, 3, 12));

        assertThat(purchaseOrderItem.getReceivedQty()).isEqualTo(50);
        assertThat(purchaseOrderItem.getPurchasePrice()).isEqualByComparingTo("1.75");
        assertThat(purchaseOrderItem.getMrp()).isEqualByComparingTo("3.00");
        assertThat(purchaseOrderItem.getSellingPrice()).isEqualByComparingTo("2.50");

        ArgumentCaptor<PharmacyStockBatch> batchCaptor = ArgumentCaptor.forClass(PharmacyStockBatch.class);
        verify(pharmacyStockBatchRepository).save(batchCaptor.capture());
        PharmacyStockBatch savedBatch = batchCaptor.getValue();
        assertThat(savedBatch.getMedicine()).isSameAs(medicine);
        assertThat(savedBatch.getVendor()).isSameAs(vendor);
        assertThat(savedBatch.getBatchNo()).isEqualTo("BATCH-APR-01");
        assertThat(savedBatch.getAvailableQty()).isEqualTo(30);
        assertThat(savedBatch.getReceivedQty()).isEqualTo(30);
        assertThat(savedBatch.getPurchaseOrderItemId()).isEqualTo(801L);

        ArgumentCaptor<PharmacyStock> stockCaptor = ArgumentCaptor.forClass(PharmacyStock.class);
        verify(pharmacyStockRepository).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getAvailableQty()).isEqualTo(45);

        ArgumentCaptor<PharmacyStockTransaction> transactionCaptor = ArgumentCaptor.forClass(PharmacyStockTransaction.class);
        verify(pharmacyStockTransactionRepository).save(transactionCaptor.capture());
        PharmacyStockTransaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getTransactionType()).isEqualTo("PURCHASE_RECEIPT");
        assertThat(savedTransaction.getQtyIn()).isEqualTo(30);
        assertThat(savedTransaction.getQtyOut()).isZero();
        assertThat(savedTransaction.getBalanceAfter()).isEqualTo(45);
        assertThat(savedTransaction.getReferenceType()).isEqualTo("PURCHASE_ORDER");
        assertThat(savedTransaction.getReferenceId()).isEqualTo(501L);
        assertThat(savedTransaction.getReferenceNo()).isEqualTo("PO-20260312-001");
        assertThat(savedTransaction.getNote()).isEqualTo("Weekly vendor delivery | Shelf A");
    }

    @Test
    @DisplayName("Receive rejects draft purchase orders before inventory writes")
    void receiveRejectsPurchaseOrdersInInvalidStatus() {
        PharmacyPurchaseOrder purchaseOrder = purchaseOrder(vendor(), PurchaseOrderStatus.DRAFT);

        when(pharmacyPurchaseOrderRepository.findByIdForUpdate(501L)).thenReturn(Optional.of(purchaseOrder));

        assertThatThrownBy(() -> service.receive(501L, validRequest()))
                .isInstanceOf(PurchaseOrderReceiptNotAllowedException.class)
                .hasMessageContaining("cannot be received in status DRAFT");

        verify(pharmacyPurchaseOrderItemRepository, never()).findAllForReceipt(anyLong(), anyCollection());
        verify(pharmacyStockBatchRepository, never()).save(any(PharmacyStockBatch.class));
    }

    @Test
    @DisplayName("Receive collects field-level validation errors before repository writes")
    void receiveValidatesRequestBeforePostingInventory() {
        PharmacyPurchaseOrder purchaseOrder = purchaseOrder(vendor(), PurchaseOrderStatus.APPROVED);
        PurchaseOrderReceiveRequest request = PurchaseOrderReceiveRequest.builder()
                .receiptDate(LocalDate.of(2026, 3, 12))
                .invoiceDate(LocalDate.of(2026, 3, 13))
                .items(List.of(
                        PurchaseOrderReceiveRequestItem.builder()
                                .purchaseOrderItemId(801L)
                                .batchNo("B1")
                                .expiryDate(LocalDate.of(2026, 3, 11))
                                .receivedQty(10)
                                .purchasePrice(new BigDecimal("1.00"))
                                .mrp(new BigDecimal("2.00"))
                                .sellingPrice(new BigDecimal("1.50"))
                                .build(),
                        PurchaseOrderReceiveRequestItem.builder()
                                .purchaseOrderItemId(801L)
                                .batchNo("B2")
                                .expiryDate(LocalDate.of(2026, 4, 1))
                                .receivedQty(5)
                                .purchasePrice(new BigDecimal("1.00"))
                                .mrp(new BigDecimal("2.00"))
                                .sellingPrice(new BigDecimal("1.50"))
                                .build()
                ))
                .build();

        when(pharmacyPurchaseOrderRepository.findByIdForUpdate(501L)).thenReturn(Optional.of(purchaseOrder));

        assertThatThrownBy(() -> service.receive(501L, request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException validationException = (ValidationException) ex;
                    assertThat(validationException.getDetails())
                            .extracting(ValidationException.ValidationErrorDetail::getField)
                            .contains("invoiceDate", "items[0].expiryDate", "items[1].purchaseOrderItemId");
                });

        verify(pharmacyPurchaseOrderItemRepository, never()).findAllForReceipt(anyLong(), anyCollection());
        verify(pharmacyStockBatchRepository, never()).save(any(PharmacyStockBatch.class));
    }

    @Test
    @DisplayName("Receipt history maps repository projection rows to DTOs")
    void getReceiptHistoryMapsProjectionRows() {
        when(pharmacyPurchaseOrderRepository.existsById(501L)).thenReturn(true);
        when(pharmacyStockBatchRepository.findReceiptHistoryByPurchaseOrderId(eq(501L), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(receiptHistoryProjection())));

        Page<PurchaseOrderReceiptHistoryRowDto> page = service.getReceiptHistory(501L, 0, 20);

        assertThat(page.getContent()).hasSize(1);
        PurchaseOrderReceiptHistoryRowDto row = page.getContent().get(0);
        assertThat(row.getBatchId()).isEqualTo(9001L);
        assertThat(row.getPurchaseOrderItemId()).isEqualTo(801L);
        assertThat(row.getMedicineId()).isEqualTo(101L);
        assertThat(row.getMedicineName()).isEqualTo("Paracetamol 500");
        assertThat(row.getBatchNo()).isEqualTo("BATCH-APR-01");
        assertThat(row.getReceivedQty()).isEqualTo(30);
        assertThat(row.getReceivedAt()).isEqualTo(Instant.parse("2026-03-12T11:00:00Z"));
    }

    @Test
    @DisplayName("Receipt history rejects missing purchase orders")
    void getReceiptHistoryRejectsMissingPurchaseOrder() {
        when(pharmacyPurchaseOrderRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getReceiptHistory(999L, 0, 20))
                .isInstanceOf(PurchaseOrderNotFoundException.class)
                .hasMessageContaining("999");
    }

    private PurchaseOrderReceiveRequest validRequest() {
        return PurchaseOrderReceiveRequest.builder()
                .receiptDate(LocalDate.of(2026, 3, 12))
                .items(List.of(PurchaseOrderReceiveRequestItem.builder()
                        .purchaseOrderItemId(801L)
                        .batchNo("BATCH-APR-01")
                        .expiryDate(LocalDate.of(2026, 8, 31))
                        .receivedQty(10)
                        .purchasePrice(new BigDecimal("1.00"))
                        .mrp(new BigDecimal("2.00"))
                        .sellingPrice(new BigDecimal("1.50"))
                        .build()))
                .build();
    }

    private PharmacyVendor vendor() {
        return PharmacyVendor.builder()
                .id(4L)
                .vendorName("ABC Pharma")
                .vendorCode("VEN-004")
                .active(true)
                .build();
    }

    private MdmMedicine medicine(Long id) {
        return MdmMedicine.builder()
                .id(id)
                .brand("Paracetamol 500")
                .composition("Paracetamol 500mg")
                .form("TAB")
                .isActive(true)
                .code("MED-001")
                .build();
    }

    private PharmacyPurchaseOrder purchaseOrder(PharmacyVendor vendor, PurchaseOrderStatus status) {
        return PharmacyPurchaseOrder.builder()
                .id(501L)
                .vendor(vendor)
                .poNumber("PO-20260312-001")
                .orderDate(LocalDate.of(2026, 3, 12))
                .status(status)
                .build();
    }

    private PharmacyPurchaseOrderItem purchaseOrderItem(
            PharmacyPurchaseOrder purchaseOrder,
            MdmMedicine medicine,
            int orderedQty,
            int receivedQty
    ) {
        return PharmacyPurchaseOrderItem.builder()
                .id(801L)
                .purchaseOrder(purchaseOrder)
                .medicine(medicine)
                .orderedQty(orderedQty)
                .receivedQty(receivedQty)
                .build();
    }

    private PurchaseOrderReceiptHistoryProjection receiptHistoryProjection() {
        return new PurchaseOrderReceiptHistoryProjection() {
            @Override
            public Long getBatchId() {
                return 9001L;
            }

            @Override
            public Long getPurchaseOrderItemId() {
                return 801L;
            }

            @Override
            public Long getMedicineId() {
                return 101L;
            }

            @Override
            public String getMedicineName() {
                return "Paracetamol 500";
            }

            @Override
            public String getBatchNo() {
                return "BATCH-APR-01";
            }

            @Override
            public LocalDate getExpiryDate() {
                return LocalDate.of(2026, 8, 31);
            }

            @Override
            public Integer getReceivedQty() {
                return 30;
            }

            @Override
            public BigDecimal getPurchasePrice() {
                return new BigDecimal("1.75");
            }

            @Override
            public BigDecimal getMrp() {
                return new BigDecimal("3.00");
            }

            @Override
            public BigDecimal getSellingPrice() {
                return new BigDecimal("2.50");
            }

            @Override
            public LocalDateTime getReceivedAt() {
                return LocalDateTime.of(2026, 3, 12, 11, 0);
            }
        };
    }
}
