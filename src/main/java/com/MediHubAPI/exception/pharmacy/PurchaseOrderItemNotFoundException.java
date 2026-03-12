package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class PurchaseOrderItemNotFoundException extends HospitalAPIException {
    public PurchaseOrderItemNotFoundException(Long itemId) {
        super(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_ITEM_NOT_FOUND", "Purchase order item not found: " + itemId);
    }
}
