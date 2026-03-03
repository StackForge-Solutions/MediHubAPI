package com.MediHubAPI.exception.billing;

import com.MediHubAPI.model.billing.Invoice;
import lombok.Getter;

@Getter
public class DraftUpsertNotAllowedException extends RuntimeException {

    private final Long invoiceId;
    private final Invoice.Status status;

    // Existing (keep)
    public DraftUpsertNotAllowedException(Long invoiceId, Invoice.Status status) {
        super("Invoice already issued/partially paid. Draft update not allowed.");
        this.invoiceId = invoiceId;
        this.status = status;
    }

    //  NEW (add this) - allows custom message
    public DraftUpsertNotAllowedException(String message, Long invoiceId, Invoice.Status status) {
        super(message);
        this.invoiceId = invoiceId;
        this.status = status;
    }
}
