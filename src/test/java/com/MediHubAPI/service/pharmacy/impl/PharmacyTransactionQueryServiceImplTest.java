package com.MediHubAPI.service.pharmacy.impl;

import com.MediHubAPI.dto.pharmacy.PharmacyTransactionDetailDto;
import com.MediHubAPI.dto.pharmacy.PharmacyTransactionRowDto;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.pharmacy.PharmacyTransactionNotFoundException;
import com.MediHubAPI.repository.pharmacy.PharmacyStockTransactionRepository;
import com.MediHubAPI.repository.projection.PharmacyTransactionDetailProjection;
import com.MediHubAPI.repository.projection.PharmacyTransactionRowProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyTransactionQueryServiceImplTest {

    @Mock
    private PharmacyStockTransactionRepository pharmacyStockTransactionRepository;

    @InjectMocks
    private PharmacyTransactionQueryServiceImpl service;

    @Test
    @DisplayName("Get transactions normalizes filters and maps ledger rows")
    void getTransactionsNormalizesFiltersAndMapsRows() {
        LocalDate fromDate = LocalDate.of(2026, 3, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 12);

        when(pharmacyStockTransactionRepository.searchTransactions(
                eq("PO"),
                eq(101L),
                eq(4L),
                eq("PURCHASE_RECEIPT"),
                eq("BATCH-APR"),
                eq("PURCHASE_ORDER"),
                eq(501L),
                eq(fromDate.atStartOfDay()),
                eq(toDate.plusDays(1).atStartOfDay()),
                eq(PageRequest.of(0, 20))
        )).thenReturn(new PageImpl<>(List.of(transactionRow())));

        Page<PharmacyTransactionRowDto> page = service.getTransactions(
                " PO ",
                101L,
                4L,
                "purchase_receipt",
                " BATCH-APR ",
                "purchase_order",
                501L,
                fromDate,
                toDate,
                0,
                20
        );

        assertThat(page.getContent()).hasSize(1);
        PharmacyTransactionRowDto row = page.getContent().get(0);
        assertThat(row.getTransactionId()).isEqualTo(7001L);
        assertThat(row.getTransactionTime()).isEqualTo(Instant.parse("2026-03-12T11:00:00Z"));
        assertThat(row.getMedicineName()).isEqualTo("Paracetamol 500");
        assertThat(row.getVendorName()).isEqualTo("ABC Pharma");
        assertThat(row.getTransactionType()).isEqualTo("PURCHASE_RECEIPT");
        assertThat(row.getBalanceAfter()).isEqualTo(120);

        verify(pharmacyStockTransactionRepository).searchTransactions(
                "PO",
                101L,
                4L,
                "PURCHASE_RECEIPT",
                "BATCH-APR",
                "PURCHASE_ORDER",
                501L,
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay(),
                PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("Get transactions rejects invalid filter combinations")
    void getTransactionsRejectsInvalidFilterCombinations() {
        assertThatThrownBy(() -> service.getTransactions(
                "x",
                0L,
                null,
                "unknown",
                null,
                null,
                null,
                LocalDate.of(2026, 3, 12),
                LocalDate.of(2026, 3, 1),
                -1,
                101
        )).isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException validationException = (ValidationException) ex;
                    assertThat(validationException.getDetails())
                            .extracting(ValidationException.ValidationErrorDetail::getField)
                            .contains("q", "medicineId", "transactionType", "fromDate", "page", "size");
                });
    }

    @Test
    @DisplayName("Get transaction derives balance before from immutable ledger values")
    void getTransactionDerivesBalanceBefore() {
        when(pharmacyStockTransactionRepository.findTransactionDetailById(7001L))
                .thenReturn(Optional.of(transactionDetail()));

        PharmacyTransactionDetailDto detail = service.getTransaction(7001L);

        assertThat(detail.getTransactionId()).isEqualTo(7001L);
        assertThat(detail.getTransactionTime()).isEqualTo(Instant.parse("2026-03-12T11:00:00Z"));
        assertThat(detail.getVendorName()).isEqualTo("ABC Pharma");
        assertThat(detail.getQtyIn()).isEqualTo(100);
        assertThat(detail.getQtyOut()).isZero();
        assertThat(detail.getBalanceBefore()).isEqualTo(20);
        assertThat(detail.getBalanceAfter()).isEqualTo(120);
    }

    @Test
    @DisplayName("Get transaction throws not found when ledger row is missing")
    void getTransactionThrowsWhenMissing() {
        when(pharmacyStockTransactionRepository.findTransactionDetailById(7002L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransaction(7002L))
                .isInstanceOf(PharmacyTransactionNotFoundException.class)
                .hasMessageContaining("7002");
    }

    private PharmacyTransactionRowProjection transactionRow() {
        return new BaseProjection();
    }

    private PharmacyTransactionDetailProjection transactionDetail() {
        return new BaseProjection();
    }

    private static class BaseProjection implements PharmacyTransactionDetailProjection {
        @Override
        public Long getTransactionId() {
            return 7001L;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return LocalDateTime.ofInstant(Instant.parse("2026-03-12T11:00:00Z"), ZoneOffset.UTC);
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
        public Long getBatchId() {
            return 9001L;
        }

        @Override
        public String getBatchNo() {
            return "BATCH-APR-01";
        }

        @Override
        public Long getVendorId() {
            return 4L;
        }

        @Override
        public String getVendorName() {
            return "ABC Pharma";
        }

        @Override
        public String getTransactionType() {
            return "PURCHASE_RECEIPT";
        }

        @Override
        public Integer getQtyIn() {
            return 100;
        }

        @Override
        public Integer getQtyOut() {
            return 0;
        }

        @Override
        public Integer getBalanceAfter() {
            return 120;
        }

        @Override
        public BigDecimal getUnitCost() {
            return new BigDecimal("1.75");
        }

        @Override
        public BigDecimal getUnitPrice() {
            return new BigDecimal("2.50");
        }

        @Override
        public String getReferenceType() {
            return "PURCHASE_ORDER";
        }

        @Override
        public Long getReferenceId() {
            return 501L;
        }

        @Override
        public String getReferenceNo() {
            return "PO-20260312-001";
        }

        @Override
        public String getCreatedBy() {
            return "admin";
        }

        @Override
        public String getNote() {
            return "Initial receipt";
        }
    }
}
