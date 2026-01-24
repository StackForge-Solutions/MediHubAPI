package com.MediHubAPI.service.scheduling.session.port.impl;

import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleBlockDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleDayPlanDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleIntervalDTO;
import com.MediHubAPI.dto.scheduling.session.validate.ValidateRequest;
import com.MediHubAPI.dto.scheduling.session.validate.ValidateResponse;
import com.MediHubAPI.dto.scheduling.session.validate.ValidationIssueDTO;
import com.MediHubAPI.service.scheduling.session.port.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
public class ValidationServiceImpl implements ValidationService {

    @Override
    public ValidateResponse validate(ValidateRequest request) {
        List<ValidationIssueDTO> issues = new ArrayList<>();

        if (request.slotDurationMinutes() == null || request.slotDurationMinutes() <= 0) {
            issues.add(new ValidationIssueDTO("SLOT_DURATION_INVALID", "slotDurationMinutes must be > 0", "slotDurationMinutes"));
        }

        // Validate each day: time order, overlaps, dayOff rules
        for (int d = 0; d < request.days().size(); d++) {
            SessionScheduleDayPlanDTO day = request.days().get(d);

            String dayPtr = "days[" + d + "]";
            if (day.dayOff()) {
                if (day.intervals() != null && !day.intervals().isEmpty()) {
                    issues.add(new ValidationIssueDTO("DAY_OFF_HAS_INTERVALS", "DayOff cannot contain intervals", dayPtr + ".intervals"));
                }
            }

            List<SessionScheduleIntervalDTO> intervals = day.intervals() == null ? List.of() : day.intervals();
            List<SessionScheduleBlockDTO> blocks = day.blocks() == null ? List.of() : day.blocks();

            // Basic ordering checks
            for (int i = 0; i < intervals.size(); i++) {
                var it = intervals.get(i);
                if (isValidRange(it.startTime(), it.endTime())) {
                    issues.add(new ValidationIssueDTO("INTERVAL_RANGE_INVALID", "startTime must be < endTime", dayPtr + ".intervals[" + i + "]"));
                }
            }
            for (int b = 0; b < blocks.size(); b++) {
                var bl = blocks.get(b);
                if (isValidRange(bl.startTime(), bl.endTime())) {
                    issues.add(new ValidationIssueDTO("BLOCK_RANGE_INVALID", "startTime must be < endTime", dayPtr + ".blocks[" + b + "]"));
                }
            }

            // Overlap checks within intervals
            detectOverlapsIntervals(intervals, dayPtr, issues);

            // Overlap checks within blocks
            detectOverlapsBlocks(blocks, dayPtr, issues);

            // Block vs interval conflicts
            detectBlockIntervalConflicts(intervals, blocks, dayPtr, issues);
        }

        boolean valid = issues.isEmpty();
        log.debug("SessionSchedule validation: valid={}, issues={}", valid, issues.size());
        return new ValidateResponse(valid, issues);
    }

    private boolean isValidRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) return true;
        return !start.isBefore(end);
    }

    private void detectOverlapsIntervals(List<SessionScheduleIntervalDTO> intervals, String dayPtr, List<ValidationIssueDTO> issues) {
        List<IdxRange> ranges = new ArrayList<>();
        for (int i = 0; i < intervals.size(); i++) {
            var it = intervals.get(i);
            if (it.startTime() == null || it.endTime() == null) continue;
            ranges.add(new IdxRange(i, it.startTime(), it.endTime()));
        }
        ranges.sort(Comparator.comparing(r -> r.start));
        for (int i = 1; i < ranges.size(); i++) {
            IdxRange prev = ranges.get(i - 1);
            IdxRange cur = ranges.get(i);
            if (cur.start.isBefore(prev.end)) {
                issues.add(new ValidationIssueDTO(
                        "INTERVAL_OVERLAP",
                        "Intervals overlap: " + prev.start + "-" + prev.end + " with " + cur.start + "-" + cur.end,
                        dayPtr + ".intervals[" + cur.idx + "]"
                ));
            }
        }
    }

    private void detectOverlapsBlocks(List<SessionScheduleBlockDTO> blocks, String dayPtr, List<ValidationIssueDTO> issues) {
        List<IdxRange> ranges = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            var bl = blocks.get(i);
            if (bl.startTime() == null || bl.endTime() == null) continue;
            ranges.add(new IdxRange(i, bl.startTime(), bl.endTime()));
        }
        ranges.sort(Comparator.comparing(r -> r.start));
        for (int i = 1; i < ranges.size(); i++) {
            IdxRange prev = ranges.get(i - 1);
            IdxRange cur = ranges.get(i);
            if (cur.start.isBefore(prev.end)) {
                issues.add(new ValidationIssueDTO(
                        "BLOCK_OVERLAP",
                        "Blocks overlap: " + prev.start + "-" + prev.end + " with " + cur.start + "-" + cur.end,
                        dayPtr + ".blocks[" + cur.idx + "]"
                ));
            }
        }
    }

    private void detectBlockIntervalConflicts(List<SessionScheduleIntervalDTO> intervals,
                                              List<SessionScheduleBlockDTO> blocks,
                                              String dayPtr,
                                              List<ValidationIssueDTO> issues) {
        for (int i = 0; i < intervals.size(); i++) {
            var it = intervals.get(i);
            if (it.startTime() == null || it.endTime() == null) continue;

            for (SessionScheduleBlockDTO bl : blocks) {
                if (bl.startTime() == null || bl.endTime() == null) continue;

                if (overlaps(it.startTime(), it.endTime(), bl.startTime(), bl.endTime())) {
                    issues.add(new ValidationIssueDTO(
                            "BLOCK_INTERVAL_CONFLICT",
                            "Block overlaps interval: interval " + it.startTime() + "-" + it.endTime() +
                                    " vs block " + bl.startTime() + "-" + bl.endTime(),
                            dayPtr + ".intervals[" + i + "]"
                    ));
                }
            }
        }
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private record IdxRange(int idx, LocalTime start, LocalTime end) {}
}
