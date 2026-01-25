package com.MediHubAPI.mapper.scheduling.template;


import java.util.ArrayList;
import java.util.List;
import com.MediHubAPI.dto.scheduling.session.template.plan.TemplateIntervalDTO;
import com.MediHubAPI.dto.scheduling.template.get.TemplateDetailDTO;
import com.MediHubAPI.dto.scheduling.template.list.TemplateSummaryDTO;
import com.MediHubAPI.dto.scheduling.template.plan.TemplateDayPlanDTO;
import com.MediHubAPI.model.scheduling.template.ScheduleTemplate;
import com.MediHubAPI.model.scheduling.template.TemplateBlock;
import com.MediHubAPI.model.scheduling.template.TemplateDay;
import com.MediHubAPI.model.scheduling.template.TemplateInterval;
import com.MediHubAPI.model.scheduling.template.plan.TemplateBlockDTO;

public final class TemplateMapper {

    private TemplateMapper() {}

    public static TemplateSummaryDTO toSummary(ScheduleTemplate t) {
        return new TemplateSummaryDTO(
                t.getId(),
                t.getScope(),
                t.getDoctorId(),
                t.getDepartmentId(),
                t.getName(),
                t.getSlotDurationMin(),
                t.isActive(),
                t.getVersion()
        );
    }

    public static TemplateDetailDTO toDetail(ScheduleTemplate t) {
        return new TemplateDetailDTO(
                t.getId(),
                t.getScope(),
                t.getDoctorId(),
                t.getDepartmentId(),
                t.getName(),
                t.getSlotDurationMin(),
                t.isActive(),
                t.getVersion(),
                t.getCreatedBy(),
                t.getUpdatedBy(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                toDayPlans(t.getDays())
        );
    }

    public static List<TemplateDayPlanDTO> toDayPlans(List<TemplateDay> days) {
        if (days == null) return List.of();
        List<TemplateDayPlanDTO> out = new ArrayList<>();
        for (TemplateDay d : days) {
            out.add(new TemplateDayPlanDTO(
                    d.getDayOfWeek(),
                    d.isDayOff(),
                    toIntervalDtos(d.getIntervals()),
                    toBlockDtos(d.getBlocks())
            ));
        }
        return out;
    }

    private static List<TemplateIntervalDTO> toIntervalDtos(List<TemplateInterval> intervals) {
        if (intervals == null) return List.of();
        List<TemplateIntervalDTO> out = new ArrayList<>();
        for (TemplateInterval it : intervals) {
            out.add(new TemplateIntervalDTO(
                    it.getStartTime(),
                    it.getEndTime(),
                    it.getSessionType(),
                    it.getCapacity()
            ));
        }
        return out;
    }

    private static List<TemplateBlockDTO> toBlockDtos(List<TemplateBlock> blocks) {
        if (blocks == null) return List.of();
        List<TemplateBlockDTO> out = new ArrayList<>();
        for (TemplateBlock b : blocks) {
            out.add(new TemplateBlockDTO(
                    b.getBlockType(),
                    b.getStartTime(),
                    b.getEndTime(),
                    b.getReason()
            ));
        }
        return out;
    }
}

