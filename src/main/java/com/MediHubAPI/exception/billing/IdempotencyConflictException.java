package com.MediHubAPI.exception.billing;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String msg) { super(msg); }
}
