package com.MediHubAPI.util;

import java.time.LocalDate;

public final class FiscalYearUtil {
    private FiscalYearUtil() {}

    // Returns FY start year. Example: Jan 2026 -> 2025 (FY 2025-26), May 2026 -> 2026 (FY 2026-27)
    public static int currentFyStartYear(LocalDate date) {
        return (date.getMonthValue() >= 4) ? date.getYear() : (date.getYear() - 1);
    }
}
