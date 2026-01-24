package com.MediHubAPI.model.enums;

public enum MergeStrategy {
    REPLACE,              // overwrite target week schedule plan
    MERGE_SKIP_CONFLICTS  // keep target, add only non-overlapping intervals/blocks
}
