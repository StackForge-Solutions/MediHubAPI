package com.MediHubAPI.service.scheduling.session.port.impl;


import com.MediHubAPI.dto.scheduling.session.archive.ArchiveResponse;
import com.MediHubAPI.dto.scheduling.session.bootstrap.BootstrapResponse;
import com.MediHubAPI.dto.scheduling.session.bootstrap.HolidayDTO;
import com.MediHubAPI.dto.scheduling.session.copy.CopyWeekRequest;
import com.MediHubAPI.dto.scheduling.session.copy.CopyWeekResponse;
import com.MediHubAPI.dto.scheduling.session.draft.DraftRequest;
import com.MediHubAPI.dto.scheduling.session.draft.DraftResponse;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleDetailDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleBlockDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleDayPlanDTO;
import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleIntervalDTO;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsRequest;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsResponse;
import com.MediHubAPI.dto.scheduling.session.publish.PublishConflictDTO;
import com.MediHubAPI.dto.scheduling.session.publish.PublishRequest;
import com.MediHubAPI.dto.scheduling.session.publish.PublishResponse;
import com.MediHubAPI.dto.scheduling.session.search.SearchResponse;
import com.MediHubAPI.dto.scheduling.session.search.SessionScheduleSummaryDTO;
import com.MediHubAPI.dto.scheduling.session.validate.ValidateRequest;
import com.MediHubAPI.exception.scheduling.session.SchedulingException;
import com.MediHubAPI.model.enums.MergeStrategy;
import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.ScheduleStatus;
import com.MediHubAPI.model.enums.SlotGenerationMode;
import com.MediHubAPI.model.scheduling.SessionSchedule;
import com.MediHubAPI.model.scheduling.SessionScheduleBlock;
import com.MediHubAPI.model.scheduling.SessionScheduleDay;
import com.MediHubAPI.model.scheduling.SessionScheduleInterval;
import com.MediHubAPI.repository.scheduling.session.SessionScheduleRepository;
import com.MediHubAPI.scheduling.session.mapper.SessionScheduleMapper;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishResult;
import com.MediHubAPI.service.scheduling.session.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionScheduleServiceImpl implements SessionScheduleService {

    private final SessionScheduleRepository repository;

    private final ValidationService validationService;
    private final SlotGenerationService slotGenerationService;

    private final ActorProvider actorProvider;
    private final DoctorDirectoryPort doctorDirectoryPort;
    private final DepartmentDirectoryPort departmentDirectoryPort;
    private final HolidayCalendarPort holidayCalendarPort;

    @Override
    public BootstrapResponse bootstrap(Long doctorId, LocalDate weekStartISO) {
        LocalDate serverDate = LocalDate.now();

        // Seed schedules for the asked week (if provided)
        List<SessionScheduleSummaryDTO> seed = new ArrayList<>();
        if (weekStartISO != null) {
            List<SessionSchedule> found;
            if (doctorId != null) {
                found = repository.findByDoctorIdAndWeekStartDate(doctorId, weekStartISO);
            } else {
                found = repository.findByModeAndWeekStartDate(ScheduleMode.DOCTOR_OVERRIDE, weekStartISO);
            }
            seed = found.stream().map(SessionScheduleMapper::toSummary).toList();
        }

        // Holidays for that week if weekStart provided
        List<HolidayDTO> holidays = List.of();
        if (weekStartISO != null) {
            LocalDate weekEnd = weekStartISO.plusDays(6);
            holidays = holidayCalendarPort.listHolidays(weekStartISO, weekEnd);
        }

        return new BootstrapResponse(
                doctorDirectoryPort.listDoctors(),
                departmentDirectoryPort.listDepartments(),
                holidays,
                List.of(),     // templates not in STEP-1
                seed,
                serverDate
        );
    }
    @Override
    @Transactional
    public DraftResponse draft(DraftRequest request) {
        // Validate plan using pure validator
        var validateReq = new ValidateRequest(
                request.mode(),
                request.doctorId(),
                request.departmentId(),
                request.weekStartDate(),
                request.slotDurationMinutes(),
                request.days()
        );

        var validation = validationService.validate(validateReq);
        if (!validation.valid()) {
            throw SchedulingException.badRequest("SCHEDULE_INVALID", "Draft validation failed: " + validation.issues().size() + " issue(s)");
        }

        String actor = actorProvider.currentActor();

        SessionSchedule schedule;
        if (request.scheduleId() == null) {
            schedule = SessionSchedule.builder()
                    .mode(request.mode())
                    .doctorId(request.doctorId())
                    .departmentId(request.departmentId())
                    .weekStartDate(request.weekStartDate())
                    .slotDurationMinutes(request.slotDurationMinutes())
                    .status(ScheduleStatus.DRAFT)
                    .locked(request.locked())
                    .lockedReason(request.lockedReason())
                    .build();
            schedule.touchForCreate(actor);
        } else {
            schedule = repository.findById(request.scheduleId())
                    .orElseThrow(() -> SchedulingException.notFound("SCHEDULE_NOT_FOUND", "Schedule not found: " + request.scheduleId()));

            // Optimistic lock guard before touching JPA @Version
            if (!Objects.equals(schedule.getVersion(), request.version())) {
                throw SchedulingException.conflict("STALE_VERSION", "Schedule version mismatch. Expected=" + schedule.getVersion() + " provided=" + request.version());
            }
            if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
                throw SchedulingException.conflict("SCHEDULE_ARCHIVED", "Cannot edit archived schedule: " + schedule.getId());
            }
            if (schedule.getStatus() == ScheduleStatus.PUBLISHED && schedule.isLocked()) {
                throw SchedulingException.conflict("SCHEDULE_LOCKED", "Cannot edit locked published schedule: " + schedule.getId());
            }

            schedule.setMode(request.mode());
            schedule.setDoctorId(request.doctorId());
            schedule.setDepartmentId(request.departmentId());
            schedule.setWeekStartDate(request.weekStartDate());
            schedule.setSlotDurationMinutes(request.slotDurationMinutes());
            schedule.setLocked(request.locked());
            schedule.setLockedReason(request.lockedReason());
            schedule.touchForUpdate(actor);
        }

        // Map plan -> entities
        schedule.setDaysReplace(toEntityDays(schedule, request.days()));

        SessionSchedule saved = repository.save(schedule);

        log.info("SessionSchedule draft saved: scheduleId={}, doctorId={}, weekStart={}, status={}, version={}",
                saved.getId(), saved.getDoctorId(), saved.getWeekStartDate(), saved.getStatus(), saved.getVersion());

        return new DraftResponse(SessionScheduleMapper.toDetail(saved));
    }

    @Override
    @Transactional
    public PublishResponse publish(PublishRequest request) {
        SessionSchedule schedule = repository.findById(request.scheduleId())
                .orElseThrow(() -> SchedulingException.notFound("SCHEDULE_NOT_FOUND", "Schedule not found: " + request.scheduleId()));

        if (!Objects.equals(schedule.getVersion(), request.version())) {
            throw SchedulingException.conflict("STALE_VERSION", "Schedule version mismatch. Expected=" + schedule.getVersion() + " provided=" + request.version());
        }

        if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
            throw SchedulingException.conflict("SCHEDULE_ARCHIVED", "Cannot publish archived schedule: " + schedule.getId());
        }

        if (schedule.isLocked()) {
            throw SchedulingException.conflict("SCHEDULE_LOCKED", "Schedule is locked: " + schedule.getLockedReason());
        }

        // Re-validate before publish (defensive)
        var validateReq = new ValidateRequest(
                schedule.getMode(),
                schedule.getDoctorId(),
                schedule.getDepartmentId(),
                schedule.getWeekStartDate(),
                schedule.getSlotDurationMinutes(),
                SessionScheduleMapper.toDayPlans(schedule.getDays())
        );
        var validation = validationService.validate(validateReq);
        if (!validation.valid()) {
            throw SchedulingException.badRequest("SCHEDULE_INVALID", "Publish validation failed: " + validation.issues().size() + " issue(s)");
        }

        boolean dryRun = request.dryRun() || request.slotGenerationMode() == SlotGenerationMode.DRY_RUN;
        boolean failOnBooked = request.failOnBookedConflict();

        String actor = actorProvider.currentActor();
        SlotPublishResult publishResult = slotGenerationService.publish(schedule, failOnBooked, dryRun, actor);

        boolean published = !dryRun;
        if (published) {
            schedule.setStatus(ScheduleStatus.PUBLISHED);
            schedule.touchForUpdate(actor);
            schedule = repository.save(schedule);
        }

        List<PublishConflictDTO> conflicts = publishResult.conflicts() == null ? List.of()
                : publishResult.conflicts().stream()
                .map(c -> new PublishConflictDTO(c.slotKey(), c.reason()))
                .toList();

        log.info("SessionSchedule publish: scheduleId={}, doctorId={}, weekStart={}, dryRun={}, planned={}, created={}, updated={}, skipped={}, conflicts={}",
                schedule.getId(), schedule.getDoctorId(), schedule.getWeekStartDate(), dryRun,
                publishResult.totalPlanned(), publishResult.created(), publishResult.updated(), publishResult.skipped(), conflicts.size());

        String msg = dryRun
                ? "Dry run completed. No DB writes performed."
                : "Published successfully.";

        return new PublishResponse(
                schedule.getId(),
                schedule.getVersion(),
                published,
                publishResult.totalPlanned(),
                publishResult.created(),
                publishResult.updated(),
                publishResult.skipped(),
                conflicts,
                msg
        );
    }

    @Override
    @Transactional
    public CopyWeekResponse copyWeek(CopyWeekRequest request) {
        if (Objects.equals(request.sourceWeekStartISO(), request.targetWeekStartISO())) {
            throw SchedulingException.badRequest("COPY_SAME_WEEK", "sourceWeekStartISO and targetWeekStartISO cannot be same.");
        }

        // Find source schedule (doctor override, latest non-archived)
        SessionSchedule source = repository
                .findByDoctorIdAndWeekStartDateAndModeAndStatusNot(
                        request.doctorId(),
                        request.sourceWeekStartISO(),
                        ScheduleMode.DOCTOR_OVERRIDE,
                        ScheduleStatus.ARCHIVED
                )
                .orElseThrow(() -> SchedulingException.notFound("SOURCE_NOT_FOUND",
                        "Source schedule not found for doctorId=" + request.doctorId() + " weekStart=" + request.sourceWeekStartISO()));

        // Check existing target schedule
        List<SessionSchedule> existingTargets = repository.findByDoctorIdAndWeekStartDate(request.doctorId(), request.targetWeekStartISO());
        SessionSchedule target = existingTargets.stream()
                .filter(s -> s.getMode() == ScheduleMode.DOCTOR_OVERRIDE && s.getStatus() != ScheduleStatus.ARCHIVED)
                .findFirst()
                .orElse(null);

        String actor = actorProvider.currentActor();

        if (target == null) {
            target = SessionSchedule.builder()
                    .mode(ScheduleMode.DOCTOR_OVERRIDE)
                    .doctorId(request.doctorId())
                    .departmentId(source.getDepartmentId())
                    .weekStartDate(request.targetWeekStartISO())
                    .slotDurationMinutes(source.getSlotDurationMinutes())
                    .status(ScheduleStatus.DRAFT)
                    .locked(false)
                    .build();
            target.touchForCreate(actor);
            target.setDaysReplace(deepCopyDays(target, source.getDays()));
            target = repository.save(target);

            log.info("CopyWeek created: sourceScheduleId={}, targetScheduleId={}, doctorId={}, sourceWeek={}, targetWeek={}",
                    source.getId(), target.getId(), request.doctorId(), request.sourceWeekStartISO(), request.targetWeekStartISO());

            return new CopyWeekResponse(target.getId(), "Target week schedule created as DRAFT.");
        }

        if (target.getStatus() == ScheduleStatus.PUBLISHED && target.isLocked()) {
            throw SchedulingException.conflict("TARGET_LOCKED", "Target schedule is locked and cannot be modified.");
        }

        // Apply merge strategy
        if (request.mergeStrategy() == MergeStrategy.REPLACE) {
            target.setSlotDurationMinutes(source.getSlotDurationMinutes());
            target.setDepartmentId(source.getDepartmentId());
            target.setStatus(ScheduleStatus.DRAFT);
            target.touchForUpdate(actor);
            target.setDaysReplace(deepCopyDays(target, source.getDays()));
            target = repository.save(target);

            log.info("CopyWeek replaced: sourceScheduleId={}, targetScheduleId={}, doctorId={}, targetWeek={}",
                    source.getId(), target.getId(), request.doctorId(), request.targetWeekStartISO());

            return new CopyWeekResponse(target.getId(), "Target week schedule replaced and saved as DRAFT.");
        }

        // MERGE_SKIP_CONFLICTS
        mergeSkipConflicts(target, source, actor);
        target = repository.save(target);

        log.info("CopyWeek merged: sourceScheduleId={}, targetScheduleId={}, doctorId={}, targetWeek={}",
                source.getId(), target.getId(), request.doctorId(), request.targetWeekStartISO());

        return new CopyWeekResponse(target.getId(), "Target week schedule merged (skipping overlaps) and saved as DRAFT.");
    }

    @Override
    public PreviewSlotsResponse previewSlots(PreviewSlotsRequest request) {
        // Validate first
        var validateReq = new ValidateRequest(
                request.mode(),
                request.doctorId(),
                request.departmentId(),
                request.weekStartDate(),
                request.slotDurationMinutes(),
                request.days()
        );
        var validation = validationService.validate(validateReq);
        if (!validation.valid()) {
            throw SchedulingException.badRequest("SCHEDULE_INVALID", "Preview validation failed: " + validation.issues().size() + " issue(s)");
        }

        // Build a transient schedule (not saved)
        SessionSchedule transientSchedule = SessionSchedule.builder()
                .id(-1L)
                .mode(request.mode())
                .doctorId(request.doctorId())
                .departmentId(request.departmentId())
                .weekStartDate(request.weekStartDate())
                .slotDurationMinutes(request.slotDurationMinutes())
                .status(ScheduleStatus.DRAFT)
                .locked(false)
                .build();

        transientSchedule.setDaysReplace(toEntityDays(transientSchedule, request.days()));
        return slotGenerationService.preview(transientSchedule);
    }

    @Override
    public SessionScheduleDetailDTO getById(Long scheduleId) {
        SessionSchedule schedule = repository.findById(scheduleId)
                .orElseThrow(() -> SchedulingException.notFound("SCHEDULE_NOT_FOUND", "Schedule not found: " + scheduleId));
        return SessionScheduleMapper.toDetail(schedule);
    }

    @Override
    public SearchResponse search(ScheduleMode mode, Long doctorId, LocalDate weekStartISO) {
        if (mode == null) {
            throw SchedulingException.badRequest("MODE_REQUIRED", "mode is required");
        }
        if (weekStartISO == null) {
            throw SchedulingException.badRequest("WEEK_START_REQUIRED", "weekStartISO is required");
        }

        List<SessionSchedule> found;
        if (doctorId != null) {
            found = repository.findByModeAndDoctorIdAndWeekStartDate(mode, doctorId, weekStartISO);
        } else {
            found = repository.findByModeAndWeekStartDate(mode, weekStartISO);
        }

        List<SessionScheduleSummaryDTO> items = found.stream()
                .map(SessionScheduleMapper::toSummary)
                .toList();

        return new SearchResponse(items);
    }

    @Override
    @Transactional
    public ArchiveResponse archive(Long scheduleId, Long version) {
        SessionSchedule schedule = repository.findById(scheduleId)
                .orElseThrow(() -> SchedulingException.notFound("SCHEDULE_NOT_FOUND", "Schedule not found: " + scheduleId));

        if (!Objects.equals(schedule.getVersion(), version)) {
            throw SchedulingException.conflict("STALE_VERSION", "Schedule version mismatch. Expected=" + schedule.getVersion() + " provided=" + version);
        }
        if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
            return new ArchiveResponse(schedule.getId(), schedule.getVersion(), "Already archived.");
        }

        String actor = actorProvider.currentActor();
        schedule.setStatus(ScheduleStatus.ARCHIVED);
        schedule.touchForUpdate(actor);
        schedule = repository.save(schedule);

        log.info("SessionSchedule archived: scheduleId={}, doctorId={}, weekStart={}, version={}",
                schedule.getId(), schedule.getDoctorId(), schedule.getWeekStartDate(), schedule.getVersion());

        return new ArchiveResponse(schedule.getId(), schedule.getVersion(), "Archived successfully.");
    }

    // -----------------------
    // Helpers: mapping + merge
    // -----------------------

    private List<SessionScheduleDay> toEntityDays(SessionSchedule schedule, List<SessionScheduleDayPlanDTO> plans) {
        // Ensure all 7 days exist or accept provided subset (we accept provided subset)
        List<SessionScheduleDay> out = new ArrayList<>();
        for (SessionScheduleDayPlanDTO p : plans) {
            SessionScheduleDay d = SessionScheduleDay.builder()
                    .schedule(schedule)
                    .dayOfWeek(p.dayOfWeek())
                    .dayOff(p.dayOff())
                    .build();

            d.setIntervalsReplace(toIntervals(d, p.intervals()));
            d.setBlocksReplace(toBlocks(d, p.blocks()));
            out.add(d);
        }
        return out;
    }

    private List<SessionScheduleInterval> toIntervals(SessionScheduleDay day, List<SessionScheduleIntervalDTO> intervals) {
        if (intervals == null) return List.of();
        List<SessionScheduleInterval> out = new ArrayList<>();
        for (SessionScheduleIntervalDTO it : intervals) {
            out.add(SessionScheduleInterval.builder()
                    .day(day)
                    .startTime(it.startTime())
                    .endTime(it.endTime())
                    .sessionType(it.sessionType())
                    .capacity(it.capacity())
                    .build());
        }
        return out;
    }

    private List<SessionScheduleBlock> toBlocks(SessionScheduleDay day, List<SessionScheduleBlockDTO> blocks) {
        if (blocks == null) return List.of();
        List<SessionScheduleBlock> out = new ArrayList<>();
        for (SessionScheduleBlockDTO b : blocks) {
            out.add(SessionScheduleBlock.builder()
                    .day(day)
                    .blockType(b.blockType())
                    .startTime(b.startTime())
                    .endTime(b.endTime())
                    .reason(b.reason())
                    .build());
        }
        return out;
    }

    private List<SessionScheduleDay> deepCopyDays(SessionSchedule targetSchedule, List<SessionScheduleDay> sourceDays) {
        List<SessionScheduleDay> out = new ArrayList<>();
        for (SessionScheduleDay sd : sourceDays) {
            SessionScheduleDay td = SessionScheduleDay.builder()
                    .schedule(targetSchedule)
                    .dayOfWeek(sd.getDayOfWeek())
                    .dayOff(sd.isDayOff())
                    .build();

            // Copy intervals
            List<SessionScheduleInterval> intervals = new ArrayList<>();
            for (SessionScheduleInterval si : sd.getIntervals()) {
                intervals.add(SessionScheduleInterval.builder()
                        .day(td)
                        .startTime(si.getStartTime())
                        .endTime(si.getEndTime())
                        .sessionType(si.getSessionType())
                        .capacity(si.getCapacity())
                        .build());
            }
            td.setIntervalsReplace(intervals);

            // Copy blocks
            List<SessionScheduleBlock> blocks = new ArrayList<>();
            for (SessionScheduleBlock sb : sd.getBlocks()) {
                blocks.add(SessionScheduleBlock.builder()
                        .day(td)
                        .blockType(sb.getBlockType())
                        .startTime(sb.getStartTime())
                        .endTime(sb.getEndTime())
                        .reason(sb.getReason())
                        .build());
            }
            td.setBlocksReplace(blocks);

            out.add(td);
        }
        return out;
    }

    private void mergeSkipConflicts(SessionSchedule target, SessionSchedule source, String actor) {
        // Merge day-wise; skip overlaps in target.
        Map<DayOfWeek, SessionScheduleDay> targetMap = target.getDays().stream()
                .collect(Collectors.toMap(SessionScheduleDay::getDayOfWeek, d -> d, (a, b) -> a, LinkedHashMap::new));

        for (SessionScheduleDay sourceDay : source.getDays()) {
            SessionScheduleDay targetDay = targetMap.get(sourceDay.getDayOfWeek());
            if (targetDay == null) {
                // create new day
                SessionScheduleDay newDay = SessionScheduleDay.builder()
                        .schedule(target)
                        .dayOfWeek(sourceDay.getDayOfWeek())
                        .dayOff(sourceDay.isDayOff())
                        .build();
                newDay.setIntervalsReplace(deepCopyDays(target, List.of(sourceDay)).get(0).getIntervals());
                newDay.setBlocksReplace(deepCopyDays(target, List.of(sourceDay)).get(0).getBlocks());
                target.getDays().add(newDay);
                continue;
            }

            // If target dayOff, keep it dayOff
            if (targetDay.isDayOff()) continue;

            // Merge intervals
            for (SessionScheduleInterval si : sourceDay.getIntervals()) {
                boolean overlaps = targetDay.getIntervals().stream().anyMatch(ti ->
                        si.getStartTime().isBefore(ti.getEndTime()) && ti.getStartTime().isBefore(si.getEndTime())
                );
                if (!overlaps) {
                    targetDay.getIntervals().add(SessionScheduleInterval.builder()
                            .day(targetDay)
                            .startTime(si.getStartTime())
                            .endTime(si.getEndTime())
                            .sessionType(si.getSessionType())
                            .capacity(si.getCapacity())
                            .build());
                }
            }

            // Merge blocks
            for (SessionScheduleBlock sb : sourceDay.getBlocks()) {
                boolean overlaps = targetDay.getBlocks().stream().anyMatch(tb ->
                        sb.getStartTime().isBefore(tb.getEndTime()) && tb.getStartTime().isBefore(sb.getEndTime())
                );
                if (!overlaps) {
                    targetDay.getBlocks().add(SessionScheduleBlock.builder()
                            .day(targetDay)
                            .blockType(sb.getBlockType())
                            .startTime(sb.getStartTime())
                            .endTime(sb.getEndTime())
                            .reason(sb.getReason())
                            .build());
                }
            }
        }

        target.setStatus(ScheduleStatus.DRAFT);
        target.touchForUpdate(actor);
    }
}