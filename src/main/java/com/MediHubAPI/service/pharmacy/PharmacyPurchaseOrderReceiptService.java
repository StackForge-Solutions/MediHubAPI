package com.MediHubAPI.service.pharmacy;

import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiptHistoryRowDto;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveRequest;
import com.MediHubAPI.dto.pharmacy.PurchaseOrderReceiveResponseDto;
import org.springframework.data.domain.Page;

public interface PharmacyPurchaseOrderReceiptService {
    PurchaseOrderReceiveResponseDto receive(Long purchaseOrderId, PurchaseOrderReceiveRequest request);

    Page<PurchaseOrderReceiptHistoryRowDto> getReceiptHistory(Long purchaseOrderId, Integer page, Integer size);
}
