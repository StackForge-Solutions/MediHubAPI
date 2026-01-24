package com.MediHubAPI.dto.scheduling.session.validate;

public record ValidationIssueDTO(
        String code,
        String message,
        String pointer // e.g., "days[0].intervals[1]"
) {}
