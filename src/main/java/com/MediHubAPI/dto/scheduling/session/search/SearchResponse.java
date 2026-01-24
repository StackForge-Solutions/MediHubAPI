package com.MediHubAPI.dto.scheduling.session.search;

import java.util.List;

public record SearchResponse(
        List<SessionScheduleSummaryDTO> items
) {}
