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
import java.util.Comparator;
import java.util.List;

@UtilityClass
public class BootstrapSeedWeeklyMapper {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DEFAULT_CHANNELS = List.of("STAFF", "ONLINE", "CALL_CENTER");
    private static final String DEFAULT_ROOM = "OPD-101";
    private static final String TZ = "Asia/Kolkata";

    public static SeedWeeklyScheduleDTO toSeedWeekly(
            SessionSchedule schedule,
            LocalDate weekStartMonday,
            Long templateId,
            Boolean inheritTemplate,
            String notes,
            String originForSessions // "LOCAL" or "OVERRIDDEN"
    ) {
        LocalDate weekEnd = weekStartMonday.plusDays(6);

        String deptId = schedule.getDepartmentId() == null ? null : String.valueOf(schedule.getDepartmentId());

        List<WeeklyDayDTO> days = new ArrayList<>();

        // Ensure stable ordering MON..SUN
        List<SessionScheduleDay> sortedDays = new ArrayList<>(schedule.getDays());
        sortedDays.sort(Comparator.comparing(d -> d.getDayOfWeek().getValue()));

        for (SessionScheduleDay d : sortedDays) {
            LocalDate dateISO = weekStartMonday.plusDays(d.getDayOfWeek().getValue() - 1);

            List<WeeklySessionDTO> sessions = new ArrayList<>();
            int sessionCounter = 1;

            // sort intervals by start time
            List<SessionScheduleInterval> sortedIntervals = new ArrayList<>(d.getIntervals());
            sortedIntervals.sort(Comparator.comparing(SessionScheduleInterval::getStartTime));

            for (SessionScheduleInterval interval : sortedIntervals) {

                List<WeeklyBlockDTO> blocks = blocksOverlappingInterval(d.getBlocks(), interval);

                sessions.add(new WeeklySessionDTO(
                        "sess_" + safeId(interval.getId()),
                        "Session " + sessionCounter,
                        interval.getSessionType().name(),
                        interval.getStartTime().format(HHMM),
                        interval.getEndTime().format(HHMM),

                        // UI expects
                        schedule.getSlotDurationMin(),
                        DEFAULT_ROOM,

                        interval.getCapacity(),
                        DEFAULT_CHANNELS,
                        true,
                        blocks,

                        originForSessions,
                        false
                ));
                sessionCounter++;
            }

            days.add(new WeeklyDayDTO(
                    d.getDayOfWeek().getValue(),
                    dateISO.toString(),
                    d.isDayOff(),
                    sessions,
                    null
            ));
        }

        return new SeedWeeklyScheduleDTO(
                schedule.getId(),
                schedule.getMode(),

                weekStartMonday.toString(),
                weekEnd.toString(),

                deptId,
                templateId,
                schedule.getDoctorId(),

                TZ,
                schedule.getSlotDurationMin(),

                inheritTemplate,
                schedule.getVersion(),

                days,
                notes
        );
    }

    private static List<WeeklyBlockDTO> blocksOverlappingInterval(List<SessionScheduleBlock> blocks,
                                                                  SessionScheduleInterval interval) {
        if (blocks == null || blocks.isEmpty()) return List.of();

        List<WeeklyBlockDTO> out = new ArrayList<>();

        // sort blocks by start time
        List<SessionScheduleBlock> sorted = new ArrayList<>(blocks);
        sorted.sort(Comparator.comparing(SessionScheduleBlock::getStartTime));

        for (SessionScheduleBlock b : sorted) {

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
