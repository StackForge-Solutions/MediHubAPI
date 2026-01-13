package com.MediHubAPI.exception.billing;

public class DuplicateReceiptException extends RuntimeException {
    public DuplicateReceiptException(String msg) { super(msg); }
}
