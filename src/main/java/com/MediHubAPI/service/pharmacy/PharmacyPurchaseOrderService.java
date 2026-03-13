package com.MediHubAPI.service.pharmacy;

import com.MediHubAPI.dto.pharmacy.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface PharmacyPurchaseOrderService {
    Page<PurchaseOrderRowDto> getPurchaseOrders(String q,
                                                String status,
                                                Long vendorId,
                                                LocalDate fromDate,
                                                LocalDate toDate,
                                                Integer page,
                                                Integer size,
                                                String sort);

    PurchaseOrderDetailDto createPurchaseOrder(PurchaseOrderCreateRequest request);

    PurchaseOrderDetailDto updatePurchaseOrder(Long purchaseOrderId, PurchaseOrderUpdateRequest request);

    PurchaseOrderDetailDto getPurchaseOrder(Long purchaseOrderId);

    PurchaseOrderActionResponseDto approvePurchaseOrder(Long purchaseOrderId);

    PurchaseOrderActionResponseDto cancelPurchaseOrder(Long purchaseOrderId);
}
