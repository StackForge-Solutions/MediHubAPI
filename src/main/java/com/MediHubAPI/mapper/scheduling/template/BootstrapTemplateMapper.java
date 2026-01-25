package com.MediHubAPI.mapper.scheduling.template;


import com.MediHubAPI.dto.scheduling.session.bootstrap.*;
import com.MediHubAPI.model.scheduling.session.SessionSchedule;
import com.MediHubAPI.model.scheduling.session.SessionScheduleBlock;
import com.MediHubAPI.model.scheduling.session.SessionScheduleDay;
import com.MediHubAPI.model.scheduling.session.SessionScheduleInterval;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class BootstrapTemplateMapper {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DEFAULT_CHANNELS = List.of("STAFF", "ONLINE", "CALL_CENTER");

    public static TemplateLiteDTO toTemplateLite(SessionSchedule schedule, LocalDate weekStartMonday) {

        LocalDate weekEnd = weekStartMonday.plusDays(6);

        String dept = (schedule.getDepartmentId() == null) ? "ALL" : String.valueOf(schedule.getDepartmentId());

        // IMPORTANT:
        // Your SessionSchedule entity (from paste) has NO "name" field.
        // So we safely return a default constant. If you later add schedule.getName(), update here.
        String name = "Default OPD";

        List<WeeklyDayDTO> days = new ArrayList<>();

        for (SessionScheduleDay d : schedule.getDays()) {

            // DayOfWeek.getValue(): MON=1..SUN=7 => add (value-1) days from Monday
            LocalDate dateISO = weekStartMonday.plusDays(d.getDayOfWeek().getValue() - 1);

            List<WeeklySessionDTO> sessions = new ArrayList<>();
            int sessionCounter = 1;

            for (SessionScheduleInterval interval : d.getIntervals()) {

                List<WeeklyBlockDTO> blocks = blocksOverlappingInterval(d.getBlocks(), interval);

                sessions.add(new WeeklySessionDTO(
                        "sess_" + safeId(interval.getId()),
                        "Session " + sessionCounter,
                        interval.getSessionType().name(),
                        interval.getStartTime().format(HHMM),
                        interval.getEndTime().format(HHMM),
                        interval.getCapacity(),
                        DEFAULT_CHANNELS,
                        true,
                        blocks
                ));

                sessionCounter++;
            }

            days.add(new WeeklyDayDTO(
                    d.getDayOfWeek().getValue(),
                    dateISO.toString(),
                    d.isDayOff(),
                    sessions
            ));
        }

        WeeklyTemplateDTO weekly = new WeeklyTemplateDTO(
                weekStartMonday.toString(),
                weekEnd.toString(),
                schedule.getDepartmentId() == null ? null : String.valueOf(schedule.getDepartmentId()),
                "Asia/Kolkata",
                schedule.getSlotDurationMinutes(),
                schedule.getVersion(),
                days
        );

        return new TemplateLiteDTO(
                schedule.getId(),
                name,
                dept,
                weekly
        );
    }

    private static List<WeeklyBlockDTO> blocksOverlappingInterval(List<SessionScheduleBlock> blocks, SessionScheduleInterval interval) {
        if (blocks == null || blocks.isEmpty()) return List.of();

        List<WeeklyBlockDTO> out = new ArrayList<>();
        for (SessionScheduleBlock b : blocks) {

            // Overlap rule:
            // b.start < interval.end AND interval.start < b.end
            boolean overlaps = b.getStartTime().isBefore(interval.getEndTime())
                    && interval.getStartTime().isBefore(b.getEndTime());

            if (!overlaps) continue;

            String type = b.getBlockType() == null ? "CUSTOM" : b.getBlockType().name();
            String reason = b.getReason();

            out.add(new WeeklyBlockDTO(
                    "blk_" + safeId(b.getId()),
                    type,
                    (reason == null || reason.isBlank()) ? type : reason,
                    reason,
                    b.getStartTime().format(HHMM),
                    b.getEndTime().format(HHMM)
            ));
        }
        return out;
    }

    private static String safeId(Long id) {
        return id == null ? "x" : String.valueOf(id);
    }
}
