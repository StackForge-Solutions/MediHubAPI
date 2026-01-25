package com.MediHubAPI.dto.scheduling.session.bootstrap;


import java.util.List;

public record WeeklySessionDTO(
        String id,              // "sess_<intervalId>"
        String label,           // "Session N"
        String type,            // OPD/VIDEO/PROCEDURE/CUSTOM
        String startHHmm,       // "HH:mm"
        String endHHmm,         // "HH:mm"
        Integer slotDurationOverrideMin,
        String roomNo,
        Integer capacity,
        List<String> channelsAllowed,
        boolean walkinAllowed,
        List<WeeklyBlockDTO> blocks,
        String origin,   // "LOCAL"/"OVERRIDDEN"/"INHERITED"
        boolean removed  // default false
) {}
