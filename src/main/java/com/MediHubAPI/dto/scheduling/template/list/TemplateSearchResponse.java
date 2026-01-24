package com.MediHubAPI.dto.scheduling.template.list;


import java.util.List;

public record TemplateSearchResponse(
        List<TemplateSummaryDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
