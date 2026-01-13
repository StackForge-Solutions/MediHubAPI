package com.MediHubAPI.exception.billing;


public class DuplicateTxnRefException extends RuntimeException {
    public DuplicateTxnRefException(String msg) { super(msg); }
}
