package com.MediHubAPI.controller.pharmacy;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.pharmacy.PharmacyEnumOptionsDto;
import com.MediHubAPI.enums.pharmacy.PurchaseOrderStatus;
import com.MediHubAPI.enums.pharmacy.StockAdjustmentReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/pharmacy")
@Slf4j
public class PharmacyMetadataController {

    private static final List<String> TRANSACTION_TYPES = List.of(
            "PURCHASE_RECEIPT",
            "SALE",
            "ADJUSTMENT_IN",
            "ADJUSTMENT_OUT",
            "RETURN_TO_VENDOR",
            "RETURN_FROM_PATIENT",
            "EXPIRED",
            "DAMAGED"
    );

    @GetMapping("/enums")
    public DataResponse<PharmacyEnumOptionsDto> getEnums() {
        log.info("API call: pharmacy enums");
        PharmacyEnumOptionsDto data = PharmacyEnumOptionsDto.builder()
                .purchaseOrderStatuses(Arrays.stream(PurchaseOrderStatus.values()).map(Enum::name).toList())
                .transactionTypes(TRANSACTION_TYPES)
                .adjustmentReasons(Arrays.stream(StockAdjustmentReason.values()).map(Enum::name).toList())
                .build();
        return new DataResponse<>(data);
    }
}
