package com.MediHubAPI.service.pharmacy;

import com.MediHubAPI.dto.pharmacy.*;
import org.springframework.data.domain.Page;

public interface PharmacyStockQueryService {

    Page<ManageStockRowDto> getManageStocks(String q,
                                            Integer page,
                                            Integer size,
                                            String sort,
                                            Boolean inStockOnly,
                                            Boolean lowStockOnly,
                                            Integer expiringInDays,
                                            Long vendorId,
                                            String form);

    StockSummaryDto getStockSummary(String q, Long vendorId, String form);

    MedicineStockDetailDto getStockDetail(Long medicineId);

    Page<MedicineStockBatchDto> getStockBatches(Long medicineId, Boolean includeExpired, Integer page, Integer size);

    Page<MedicineStockTransactionDto> getStockTransactions(Long medicineId, Integer page, Integer size);
}
