package com.MediHubAPI.dto.billing;

public enum InvoiceFetchMode {
    ACTIVE,   // current valid invoice (not cancelled)
    LATEST ,   // last generated invoice (even if cancelled)
    DRAFT   // ✅ build from prescribed_tests

    }
