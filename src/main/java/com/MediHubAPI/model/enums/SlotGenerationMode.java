package com.MediHubAPI.model.enums;
public enum SlotGenerationMode {
    SAFE_IDEMPOTENT,      // create missing, update safe, never delete booked
    DRY_RUN               // no DB writes (preview)
}
