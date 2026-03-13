package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class StockAdjustmentNotFoundException extends HospitalAPIException {
    public StockAdjustmentNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "STOCK_ADJUSTMENT_NOT_FOUND", "Stock adjustment not found: " + id);
    }
}
