package com.MediHubAPI.dto.scheduling.session.draft;

import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleDetailDTO;

public record DraftResponse(
        SessionScheduleDetailDTO schedule
) {}
