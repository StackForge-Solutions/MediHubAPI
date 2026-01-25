package com.MediHubAPI.service.scheduling.template.port.impl;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.MediHubAPI.dto.scheduling.session.template.plan.TemplateIntervalDTO;
import com.MediHubAPI.dto.scheduling.template.clone.TemplateCloneRequest;
import com.MediHubAPI.dto.scheduling.template.create.TemplateCreateRequest;
import com.MediHubAPI.dto.scheduling.template.get.TemplateDetailDTO;
import com.MediHubAPI.dto.scheduling.template.list.TemplateSearchResponse;
import com.MediHubAPI.dto.scheduling.template.list.TemplateSummaryDTO;
import com.MediHubAPI.dto.scheduling.template.plan.TemplateDayPlanDTO;
import com.MediHubAPI.dto.scheduling.template.update.TemplateUpdateRequest;
import com.MediHubAPI.exception.scheduling.session.SchedulingException;
import com.MediHubAPI.mapper.scheduling.template.TemplateMapper;
import com.MediHubAPI.model.enums.TemplateScope;
import com.MediHubAPI.model.scheduling.session.SessionSchedule;
import com.MediHubAPI.model.scheduling.session.SessionScheduleBlock;
import com.MediHubAPI.model.scheduling.session.SessionScheduleDay;
import com.MediHubAPI.model.scheduling.session.SessionScheduleInterval;
import com.MediHubAPI.model.scheduling.template.ScheduleTemplate;
import com.MediHubAPI.model.scheduling.template.TemplateBlock;
import com.MediHubAPI.model.scheduling.template.TemplateDay;
import com.MediHubAPI.model.scheduling.template.TemplateInterval;
import com.MediHubAPI.model.scheduling.template.plan.TemplateBlockDTO;
import com.MediHubAPI.repository.scheduling.session.SessionScheduleRepository;
import com.MediHubAPI.repository.scheduling.template.ScheduleTemplateRepository;
import com.MediHubAPI.repository.scheduling.template.TemplateSpecifications;
import com.MediHubAPI.service.scheduling.session.port.ActorProvider;
import com.MediHubAPI.service.scheduling.template.port.TemplateService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final ScheduleTemplateRepository repository;

    // Needed only for "create from schedule"
    private final SessionScheduleRepository sessionScheduleRepository;

    // Reuse existing actor provider (already required by STEP-1)
    private final ActorProvider actorProvider;

    @Override
    public TemplateSearchResponse search(TemplateScope scope, Long doctorId, Long departmentId, Boolean active,
            String q, Pageable pageable) {

        Specification<ScheduleTemplate> spec = Specification.where(TemplateSpecifications.scopeEq(scope)).and(
                TemplateSpecifications.doctorIdEq(doctorId)).and(
                TemplateSpecifications.departmentIdEq(departmentId)).and(TemplateSpecifications.activeEq(active)).and(
                TemplateSpecifications.nameLike(q));

        Page<ScheduleTemplate> page = repository.findAll(spec, pageable);
        List<TemplateSummaryDTO> items = page.getContent().stream().map(TemplateMapper::toSummary).toList();

        return new TemplateSearchResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    @Transactional
    public TemplateDetailDTO create(TemplateCreateRequest request) {
        validateScope(request.scope(), request.doctorId(), request.departmentId());

        String actor = actorProvider.currentActor();

        ScheduleTemplate t = ScheduleTemplate.builder().scope(request.scope()).doctorId(
                request.doctorId()).departmentId(request.departmentId()).name(request.name()).slotDurationMin(
                request.slotDurationMin()).active(Boolean.TRUE.equals(request.active())).build();
        t.touchForCreate(actor);

        List<TemplateDayPlanDTO> planDays = request.days();

        // Optional: create from an existing SessionSchedule
        if (request.sourceScheduleId() != null) {
            SessionSchedule schedule = sessionScheduleRepository.findById(request.sourceScheduleId()).orElseThrow(
                    () -> SchedulingException.notFound("SCHEDULE_NOT_FOUND",
                            "Source schedule not found: " + request.sourceScheduleId()));
            planDays = mapScheduleToTemplatePlans(schedule);
        }

        validatePlan(planDays);

        t.setDaysReplace(toEntityDays(t, planDays));

        ScheduleTemplate saved = repository.save(t);

        log.info("Template created: templateId={}, scope={}, doctorId={}, departmentId={}, version={}", saved.getId(),
                saved.getScope(), saved.getDoctorId(), saved.getDepartmentId(), saved.getVersion());

        return TemplateMapper.toDetail(saved);
    }

    @Override
    public TemplateDetailDTO getById(Long templateId) {
        ScheduleTemplate t = repository.findById(templateId).orElseThrow(
                () -> SchedulingException.notFound("TEMPLATE_NOT_FOUND", "Template not found: " + templateId));
        return TemplateMapper.toDetail(t);
    }

    @Override
    @Transactional
    public TemplateDetailDTO update(Long templateId, TemplateUpdateRequest request) {
        ScheduleTemplate t = repository.findById(templateId).orElseThrow(
                () -> SchedulingException.notFound("TEMPLATE_NOT_FOUND", "Template not found: " + templateId));

        if (!Objects.equals(t.getVersion(), request.version())) {
            throw SchedulingException.conflict("STALE_VERSION",
                    "Template version mismatch. Expected=" + t.getVersion() + " provided=" + request.version());
        }

        validateScope(request.scope(), request.doctorId(), request.departmentId());
        validatePlan(request.days());

        String actor = actorProvider.currentActor();

        t.setScope(request.scope());
        t.setDoctorId(request.doctorId());
        t.setDepartmentId(request.departmentId());
        t.setName(request.name());
        t.setSlotDurationMin(request.slotDurationMin());
        t.setActive(Boolean.TRUE.equals(request.active()));
        t.touchForUpdate(actor);

        t.setDaysReplace(toEntityDays(t, request.days()));

        ScheduleTemplate saved = repository.save(t);

        log.info("Template updated: templateId={}, scope={}, doctorId={}, departmentId={}, version={}", saved.getId(),
                saved.getScope(), saved.getDoctorId(), saved.getDepartmentId(), saved.getVersion());

        return TemplateMapper.toDetail(saved);
    }

    @Override
    @Transactional
    public TemplateDetailDTO cloneTemplate(Long templateId, TemplateCloneRequest request) {
        ScheduleTemplate source = repository.findById(templateId).orElseThrow(
                () -> SchedulingException.notFound("TEMPLATE_NOT_FOUND", "Template not found: " + templateId));

        if (!Objects.equals(source.getVersion(), request.sourceVersion())) {
            throw SchedulingException.conflict("STALE_VERSION",
                    "Source template version mismatch. Expected=" + source.getVersion() + " provided=" +
                            request.sourceVersion());
        }

        validateScope(request.targetScope(), request.targetDoctorId(), request.targetDepartmentId());

        String actor = actorProvider.currentActor();

        ScheduleTemplate copy = ScheduleTemplate.builder().scope(request.targetScope()).doctorId(
                request.targetDoctorId()).departmentId(request.targetDepartmentId()).name(
                request.newName()).slotDurationMin(source.getSlotDurationMin()).active(
                request.active()).build();
        copy.touchForCreate(actor);

        // Deep copy children
        List<TemplateDayPlanDTO> plan = TemplateMapper.toDayPlans(source.getDays());
        validatePlan(plan);

        copy.setDaysReplace(toEntityDays(copy, plan));

        ScheduleTemplate saved = repository.save(copy);

        log.info("Template cloned: sourceTemplateId={}, newTemplateId={}, scope={}, doctorId={}, departmentId={}",
                source.getId(), saved.getId(), saved.getScope(), saved.getDoctorId(), saved.getDepartmentId());

        return TemplateMapper.toDetail(saved);
    }

    // -------------------------
    // Validation rules (explicit)
    // -------------------------

    private void validateScope(TemplateScope scope, Long doctorId, Long departmentId) {
        if (scope == TemplateScope.DOCTOR && doctorId == null) {
            throw SchedulingException.badRequest("DOCTOR_ID_REQUIRED", "doctorId is required when scope=DOCTOR");
        }
        if (scope == TemplateScope.DEPARTMENT && departmentId == null) {
            throw SchedulingException.badRequest("DEPARTMENT_ID_REQUIRED",
                    "departmentId is required when scope=DEPARTMENT");
        }
//        if (scope == TemplateScope.GLOBAL) {
//            // keep both ids null for global (clean)
//        }
    }

    /**
     * Explicit rule:
     * - Intervals must NOT overlap each other (per day)
     * - Blocks must NOT overlap each other (per day)
     * - Blocks ARE ALLOWED to overlap intervals (this is how lunch/ward rounds work)
     */
    private void validatePlan(List<TemplateDayPlanDTO> days) {
        if (days == null || days.isEmpty()) {
            throw SchedulingException.badRequest("DAYS_REQUIRED", "At least 1 day plan is required.");
        }

        // Ensure no duplicate dayOfWeek
        Set<String> seen = new HashSet<>();
        for (TemplateDayPlanDTO day : days) {
            String key = day.dayOfWeek().name();
            if (!seen.add(key)) {
                throw SchedulingException.badRequest("DUPLICATE_DAY", "Duplicate dayOfWeek found: " + key);
            }

            List<TemplateIntervalDTO> intervals = day.intervals() == null ? List.of() : day.intervals();
            List<TemplateBlockDTO> blocks = day.blocks() == null ? List.of() : day.blocks();

            // Day-off means no intervals/blocks
            if (day.dayOff() && (!intervals.isEmpty() || !blocks.isEmpty())) {
                throw SchedulingException.badRequest("DAY_OFF_HAS_RULES",
                        "dayOff=true but intervals/blocks provided for " + key);
            }

            // Validate time ranges
            for (TemplateIntervalDTO it : intervals) {
                if (!it.startTime().isBefore(it.endTime())) {
                    throw SchedulingException.badRequest("INVALID_INTERVAL_TIME",
                            "Interval startTime must be before endTime for " + key);
                }
            }
            for (TemplateBlockDTO b : blocks) {
                if (!b.startTime().isBefore(b.endTime())) {
                    throw SchedulingException.badRequest("INVALID_BLOCK_TIME",
                            "Block startTime must be before endTime for " + key);
                }
            }

            // Overlap checks (interval vs interval)
            if (hasOverlapIntervals(intervals)) {
                throw SchedulingException.conflict("INTERVAL_OVERLAP", "Intervals overlap for " + key);
            }

            // Overlap checks (block vs block)
            if (hasOverlapBlocks(blocks)) {
                throw SchedulingException.conflict("BLOCK_OVERLAP", "Blocks overlap for " + key);
            }

            // NOTE: Blocks can overlap intervals — allowed by rule.
        }
    }

    private boolean hasOverlapIntervals(List<TemplateIntervalDTO> intervals) {
        if (intervals.size() <= 1) return false;
        List<TemplateIntervalDTO> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparing(TemplateIntervalDTO::startTime));
        for (int i = 1; i < sorted.size(); i++) {
            TemplateIntervalDTO prev = sorted.get(i - 1);
            TemplateIntervalDTO cur = sorted.get(i);
            if (cur.startTime().isBefore(prev.endTime())) return true;
        }
        return false;
    }

    private boolean hasOverlapBlocks(List<TemplateBlockDTO> blocks) {
        if (blocks.size() <= 1) return false;
        List<TemplateBlockDTO> sorted = new ArrayList<>(blocks);
        sorted.sort(Comparator.comparing(TemplateBlockDTO::startTime));
        for (int i = 1; i < sorted.size(); i++) {
            TemplateBlockDTO prev = sorted.get(i - 1);
            TemplateBlockDTO cur = sorted.get(i);
            if (cur.startTime().isBefore(prev.endTime())) return true;
        }
        return false;
    }

    // -------------------------
    // Entity mapping
    // -------------------------

    private List<TemplateDay> toEntityDays(ScheduleTemplate template, List<TemplateDayPlanDTO> plans) {
        List<TemplateDay> out = new ArrayList<>();
        for (TemplateDayPlanDTO p : plans) {
            TemplateDay d = TemplateDay.builder().template(template).dayOfWeek(p.dayOfWeek()).dayOff(
                    p.dayOff()).build();

            d.setIntervalsReplace(toIntervals(d, p.intervals()));
            d.setBlocksReplace(toBlocks(d, p.blocks()));

            out.add(d);
        }
        return out;
    }

    private List<TemplateInterval> toIntervals(TemplateDay day, List<TemplateIntervalDTO> intervals) {
        if (intervals == null) return List.of();
        List<TemplateInterval> out = new ArrayList<>();
        for (TemplateIntervalDTO it : intervals) {
            out.add(TemplateInterval.builder().day(day).startTime(it.startTime()).endTime(it.endTime())
                    .sessionType(it.sessionType()) // enum
                    .capacity(it.capacity()).build());
        }
        return out;
    }

    private List<TemplateBlock> toBlocks(TemplateDay day, List<TemplateBlockDTO> blocks) {
        if (blocks == null) return List.of();
        List<TemplateBlock> out = new ArrayList<>();
        for (TemplateBlockDTO b : blocks) {
            out.add(TemplateBlock.builder().day(day).blockType(b.blockType()).startTime(b.startTime()).endTime(
                    b.endTime()).reason(b.reason()).build());
        }
        return out;
    }

    // -------------------------
    // Create-from-Schedule mapping
    // -------------------------

    private List<TemplateDayPlanDTO> mapScheduleToTemplatePlans(SessionSchedule schedule) {
        // SessionSchedule children are your STEP-1 entities. We reuse their exact fields without touching Slot/Appointment.
        List<TemplateDayPlanDTO> out = new ArrayList<>();
        if (schedule.getDays() == null) return out;

        for (SessionScheduleDay d : schedule.getDays()) {
            List<TemplateIntervalDTO> intervals = new ArrayList<>();
            if (d.getIntervals() != null) {
                for (SessionScheduleInterval it : d.getIntervals()) {
                    intervals.add(new TemplateIntervalDTO(it.getStartTime(), it.getEndTime(),
                            it.getSessionType(), // safe stringify
                            it.getCapacity()));
                }
            }

            List<TemplateBlockDTO> blocks = new ArrayList<>();
            if (d.getBlocks() != null) {
                for (SessionScheduleBlock b : d.getBlocks()) {
                    blocks.add(new TemplateBlockDTO(b.getBlockType(), // safe stringify
                            b.getStartTime(), b.getEndTime(), b.getReason()));
                }
            }

            out.add(new TemplateDayPlanDTO(d.getDayOfWeek(), d.isDayOff(), intervals, blocks));
        }

        return out;
    }
}
