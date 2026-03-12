package com.MediHubAPI.controller.pharmacy;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.PageResponse;
import com.MediHubAPI.dto.pharmacy.CreateStockAdjustmentRequestDto;
import com.MediHubAPI.dto.pharmacy.CreateStockAdjustmentResponseDto;
import com.MediHubAPI.dto.pharmacy.StockAdjustmentDetailDto;
import com.MediHubAPI.dto.pharmacy.StockAdjustmentListRowDto;
import com.MediHubAPI.service.pharmacy.PharmacyStockAdjustmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/pharmacy/stock-adjustments")
@Slf4j
public class PharmacyStockAdjustmentController {

    private final PharmacyStockAdjustmentService pharmacyStockAdjustmentService;

    @GetMapping
    public PageResponse<StockAdjustmentListRowDto> getStockAdjustments(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("API call: stock adjustments q={}, reason={}, type={}, fromDate={}, toDate={}, page={}, size={}",
                q, reason, type, fromDate, toDate, page, size);
        return toPageResponse(pharmacyStockAdjustmentService.getStockAdjustments(q, reason, type, fromDate, toDate, page, size));
    }

    @PostMapping
    public DataResponse<CreateStockAdjustmentResponseDto> createStockAdjustment(
            @Valid @RequestBody CreateStockAdjustmentRequestDto request
    ) {
        log.info("API call: create stock adjustment date={}, type={}, reason={}, itemCount={}",
                request.getAdjustmentDate(), request.getAdjustmentType(), request.getReason(), request.getItems().size());
        return new DataResponse<>(pharmacyStockAdjustmentService.createStockAdjustment(request));
    }

    @GetMapping("/{id}")
    public DataResponse<StockAdjustmentDetailDto> getStockAdjustment(@PathVariable("id") @Positive Long adjustmentId) {
        log.info("API call: stock adjustment detail id={}", adjustmentId);
        return new DataResponse<>(pharmacyStockAdjustmentService.getStockAdjustment(adjustmentId));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
