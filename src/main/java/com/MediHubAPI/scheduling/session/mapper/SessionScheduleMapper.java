package com.MediHubAPI.scheduling.session.mapper;


import java.util.ArrayList;
import java.util.List;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleDetailDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleBlockDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleDayPlanDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleIntervalDTO;
import com.MediHubAPI.dto.scheduling.session.search.SessionScheduleSummaryDTO;
import com.MediHubAPI.model.scheduling.session.SessionSchedule;
import com.MediHubAPI.model.scheduling.session.SessionScheduleBlock;
import com.MediHubAPI.model.scheduling.session.SessionScheduleDay;
import com.MediHubAPI.model.scheduling.session.SessionScheduleInterval;

public final class SessionScheduleMapper {

    private SessionScheduleMapper() {}

    public static SessionScheduleDetailDTO toDetail(SessionSchedule s) {
        return new SessionScheduleDetailDTO(
                s.getId(),
                s.getMode(),
                s.getDoctorId(),
                s.getDepartmentId(),
                s.getWeekStartDate(),
                s.getWeekStartDate().plusDays(6),
                s.getSlotDurationMin(),
                s.getStatus(),
                s.isLocked(),
                s.getLockedReason(),
                s.getVersion(),
                s.getCreatedBy(),
                s.getUpdatedBy(),
                toDayPlans(s.getDays())
        );
    }

    public static SessionScheduleSummaryDTO toSummary(SessionSchedule s) {
        return new SessionScheduleSummaryDTO(
                s.getId(),
                s.getMode(),
                s.getDoctorId(),
                s.getDepartmentId(),
                s.getWeekStartDate(),
                s.getStatus(),
                s.isLocked(),
                s.getVersion()
        );
    }

    public static List<SessionScheduleDayPlanDTO> toDayPlans(List<SessionScheduleDay> days) {
        if (days == null) return List.of();
        List<SessionScheduleDayPlanDTO> out = new ArrayList<>();
        for (SessionScheduleDay d : days) {
            out.add(new SessionScheduleDayPlanDTO(
                    d.getDayOfWeek(),
                    d.isDayOff(),
                    toIntervals(d.getIntervals()),
                    toBlocks(d.getBlocks())
            ));
        }
        return out;
    }

    private static List<SessionScheduleIntervalDTO> toIntervals(List<SessionScheduleInterval> intervals) {
        if (intervals == null) return List.of();
        List<SessionScheduleIntervalDTO> out = new ArrayList<>();
        for (SessionScheduleInterval i : intervals) {
            out.add(new SessionScheduleIntervalDTO(
                    i.getId(),
                    i.getStartTime(),
                    i.getEndTime(),
                    i.getSessionType(),
                    i.getCapacity()
            ));
        }
        return out;
    }

    private static List<SessionScheduleBlockDTO> toBlocks(List<SessionScheduleBlock> blocks) {
        if (blocks == null) return List.of();
        List<SessionScheduleBlockDTO> out = new ArrayList<>();
        for (SessionScheduleBlock b : blocks) {
            out.add(new SessionScheduleBlockDTO(
                    b.getId(),
                    b.getBlockType(),
                    b.getStartTime(),
                    b.getEndTime(),
                    b.getReason()
            ));
        }
        return out;
    }
}
