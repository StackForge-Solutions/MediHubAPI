package com.MediHubAPI.service.scheduling.session.port.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewDayDTO;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotCellDTO;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsResponse;
import com.MediHubAPI.model.enums.BlockType;
import com.MediHubAPI.model.enums.SessionType;
import com.MediHubAPI.model.scheduling.session.SessionSchedule;
import com.MediHubAPI.model.scheduling.session.SessionScheduleBlock;
import com.MediHubAPI.model.scheduling.session.SessionScheduleDay;
import com.MediHubAPI.model.scheduling.session.SessionScheduleInterval;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishCommand;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishResult;
import com.MediHubAPI.service.scheduling.session.port.SlotGenerationService;
import com.MediHubAPI.service.scheduling.session.port.SlotPublishingPort;


@Slf4j
@Service
@RequiredArgsConstructor
public class SlotGenerationServiceImpl implements SlotGenerationService {

    private final SlotPublishingPort slotPublishingPort;

    @Override
    public PreviewSlotsResponse preview(SessionSchedule schedule) {
        var desired = buildDesiredSlots(schedule);

        // Build day wise preview grid
        Map<LocalDate, List<SlotPublishCommand.DesiredSlot>> byDate =
                desired.stream().collect(Collectors.groupingBy(SlotPublishCommand.DesiredSlot::date, LinkedHashMap::new,
                        Collectors.toList()));

        List<PreviewDayDTO> days = new ArrayList<>();
        for (SessionScheduleDay d : schedule.getDays()) {
            LocalDate date = schedule.getWeekStartDate().plusDays(d.getDayOfWeek().getValue() - 1L); // MON=1..SUN=7
            List<SlotPublishCommand.DesiredSlot> daySlots = byDate.getOrDefault(date, List.of());

            List<PreviewSlotCellDTO> cells = daySlots.stream()
                    .sorted(Comparator.comparing(SlotPublishCommand.DesiredSlot::startTime))
                    .map(s -> new PreviewSlotCellDTO(
                            s.slotKey(),
                            s.startTime(),
                            s.endTime(),
                            s.blocked(),
                            s.blockType(),
                            s.sessionType(),
                            s.capacity()
                    ))
                    .toList();

            days.add(new PreviewDayDTO(d.getDayOfWeek(), date, d.isDayOff(), cells));
        }

        return new PreviewSlotsResponse(
                schedule.getWeekStartDate(),
                schedule.getSlotDurationMinutes(),
                days,
                desired.size()
        );
    }

    @Override
    public SlotPublishResult publish(SessionSchedule schedule, boolean failOnBookedConflict, boolean dryRun,
            String actor) {
        var desired = buildDesiredSlots(schedule);
        if (dryRun) {
            return new SlotPublishResult(desired.size(), 0, 0, desired.size(),
                    desired.stream().map(s -> new SlotPublishResult.Conflict(s.slotKey(), "DRY_RUN")).toList());
        }

        if (schedule.getDoctorId() == null) {
            // In STEP-1 we only publish to Slot table for doctor schedules.
            // Global schedules can be stored as DRAFT/PUBLISHED in this module, but slot publishing needs targeting rules.
            return new SlotPublishResult(desired.size(), 0, 0, desired.size(),
                    desired.stream().map(
                            s -> new SlotPublishResult.Conflict(s.slotKey(), "NO_DOCTOR_TARGET")).toList());
        }

        SlotPublishCommand cmd = new SlotPublishCommand(
                schedule.getId(),
                schedule.getDoctorId(),
                schedule.getWeekStartDate(),
                schedule.getSlotDurationMinutes(),
                failOnBookedConflict,
                desired,
                actor
        );

        return slotPublishingPort.publishSlots(cmd);
    }

    private List<SlotPublishCommand.DesiredSlot> buildDesiredSlots(SessionSchedule schedule) {
        int duration = schedule.getSlotDurationMinutes();

        // Pre-compute block windows per day for fast lookup
        Map<LocalDate, List<TimeWindowBlock>> blocksByDate = new HashMap<>();
        for (SessionScheduleDay d : schedule.getDays()) {
            LocalDate date = schedule.getWeekStartDate().plusDays(d.getDayOfWeek().getValue() - 1L);
            List<TimeWindowBlock> list = new ArrayList<>();
            if (d.getBlocks() != null) {
                for (SessionScheduleBlock b : d.getBlocks()) {
                    list.add(new TimeWindowBlock(b.getStartTime(), b.getEndTime(), b.getBlockType(), b.getReason()));
                }
            }
            blocksByDate.put(date, list);
        }

        List<SlotPublishCommand.DesiredSlot> out = new ArrayList<>();

        for (SessionScheduleDay d : schedule.getDays()) {
            LocalDate date = schedule.getWeekStartDate().plusDays(d.getDayOfWeek().getValue() - 1L);

            if (d.isDayOff()) continue;

            List<TimeWindowBlock> dayBlocks = blocksByDate.getOrDefault(date, List.of());

            if (d.getIntervals() == null) continue;

            for (SessionScheduleInterval it : d.getIntervals()) {
                LocalTime cursor = it.getStartTime();
                while (!cursor.plusMinutes(duration).isAfter(it.getEndTime())) {
                    LocalTime slotStart = cursor;
                    LocalTime slotEnd = cursor.plusMinutes(duration);

                    // Determine if blocked
                    TimeWindowBlock block = findBlocking(dayBlocks, slotStart, slotEnd);

                    boolean blocked = block != null;
                    BlockType blockType = block != null ? block.type : null;
                    SessionType sessionType = it.getSessionType();
                    Integer capacity = it.getCapacity();

                    String slotKey = buildSlotKey(date, slotStart, slotEnd);

                    out.add(new SlotPublishCommand.DesiredSlot(
                            date, slotStart, slotEnd,
                            blocked,
                            blockType,
                            sessionType,
                            capacity,
                            slotKey
                    ));

                    cursor = slotEnd;
                }
            }
        }

        return out;
    }

    private TimeWindowBlock findBlocking(List<TimeWindowBlock> blocks, LocalTime start, LocalTime end) {
        for (TimeWindowBlock b : blocks) {
            if (start.isBefore(b.end) && b.start.isBefore(end)) {
                return b;
            }
        }
        return null;
    }

    private String buildSlotKey(LocalDate date, LocalTime start, LocalTime end) {
        return date + "|" + start + "|" + end;
    }

    private record TimeWindowBlock(LocalTime start, LocalTime end, BlockType type, String reason) {}
}