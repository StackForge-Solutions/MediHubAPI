package com.MediHubAPI.dto.scheduling.session.validate;


import java.util.List;

public record ValidateResponse(
        boolean valid,
        List<ValidationIssueDTO> issues
) {}
