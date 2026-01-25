package com.MediHubAPI.dto.scheduling.session.bootstrap;


public record WeeklyBlockDTO(
        String id,          // "blk_<blockId>"
        String type,        // LUNCH/WARD_ROUND/...
        String label,       // from reason or type
        String notes,       // from reason
        String startHHmm,   // "HH:mm"
        String endHHmm      // "HH:mm"
) {}
