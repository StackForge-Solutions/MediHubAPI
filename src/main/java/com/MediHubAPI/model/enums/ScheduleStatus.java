package com.MediHubAPI.model.enums;

/**
 * Lifecycle of a session schedule.
 */
public enum ScheduleStatus {
    /** Work-in-progress schedule; editable and not yet live. */
    DRAFT,
    /** Live schedule; used for slot generation and patient-facing availability. */
    PUBLISHED,
    /** Retired schedule kept only for history/audit and excluded from active lookups. */
    ARCHIVED
}
