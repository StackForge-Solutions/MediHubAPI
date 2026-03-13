package com.MediHubAPI.service.pharmacy;

import com.MediHubAPI.dto.pharmacy.CreateStockAdjustmentRequestDto;
import com.MediHubAPI.dto.pharmacy.CreateStockAdjustmentResponseDto;
import com.MediHubAPI.dto.pharmacy.StockAdjustmentDetailDto;
import com.MediHubAPI.dto.pharmacy.StockAdjustmentListRowDto;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface PharmacyStockAdjustmentService {
    Page<StockAdjustmentListRowDto> getStockAdjustments(String q,
                                                        String reason,
                                                        String type,
                                                        LocalDate fromDate,
                                                        LocalDate toDate,
                                                        Integer page,
                                                        Integer size);

    CreateStockAdjustmentResponseDto createStockAdjustment(CreateStockAdjustmentRequestDto request);

    StockAdjustmentDetailDto getStockAdjustment(Long adjustmentId);
}
