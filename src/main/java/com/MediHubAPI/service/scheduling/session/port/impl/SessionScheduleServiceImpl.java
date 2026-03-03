package com.MediHubAPI.service.scheduling.session.port.impl;


import com.MediHubAPI.dto.scheduling.session.archive.ArchiveResponse;
import com.MediHubAPI.dto.scheduling.session.bootstrap.BootstrapResponse;
import com.MediHubAPI.dto.scheduling.session.bootstrap.HolidayDTO;
import com.MediHubAPI.dto.scheduling.session.bootstrap.SeedWeeklyScheduleDTO;
import com.MediHubAPI.dto.scheduling.session.bootstrap.TemplateLiteDTO;
import com.MediHubAPI.dto.scheduling.session.copy.CopyFromWeekRequest;
import com.MediHubAPI.dto.scheduling.session.copy.CopyFromWeekResponse;
import com.MediHubAPI.dto.scheduling.session.copy.CopyLastWeekRequest;
import com.MediHubAPI.dto.scheduling.session.copy.CopyWeekResponse;
import com.MediHubAPI.dto.scheduling.session.draft.DraftRequest;
import com.MediHubAPI.dto.scheduling.session.draft.DraftResponse;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleDetailDTO;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleVersionDTO;
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
import com.MediHubAPI.mapper.scheduling.template.BootstrapSeedWeeklyMapper;
import com.MediHubAPI.mapper.scheduling.template.BootstrapTemplateMapper;
import com.MediHubAPI.model.enums.CopyStrategy;
import com.MediHubAPI.model.enums.MergeStrategy;
import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.ScheduleStatus;
import com.MediHubAPI.model.enums.SlotGenerationMode;
import com.MediHubAPI.model.scheduling.session.SessionSchedule;
import com.MediHubAPI.model.scheduling.session.SessionScheduleBlock;
import com.MediHubAPI.model.scheduling.session.SessionScheduleDay;
import com.MediHubAPI.model.scheduling.session.SessionScheduleInterval;
import com.MediHubAPI.repository.scheduling.session.SessionScheduleRepository;
import com.MediHubAPI.scheduling.session.mapper.SessionScheduleMapper;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishResult;
import com.MediHubAPI.service.scheduling.session.port.ActorProvider;
import com.MediHubAPI.service.scheduling.session.port.DepartmentDirectoryPort;
import com.MediHubAPI.service.scheduling.session.port.DoctorDirectoryPort;
import com.MediHubAPI.service.scheduling.session.port.HolidayCalendarPort;
import com.MediHubAPI.service.scheduling.session.port.SessionScheduleService;
import com.MediHubAPI.service.scheduling.session.port.SlotGenerationService;
import com.MediHubAPI.service.scheduling.session.port.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public
class SessionScheduleServiceImpl implements SessionScheduleService {

    private final SessionScheduleRepository sessionScheduleRepository;

    private final ValidationService     validationService;
    private final SlotGenerationService slotGenerationService;

    private final ActorProvider           actorProvider;
    private final DoctorDirectoryPort     doctorDirectoryPort;
    private final DepartmentDirectoryPort departmentDirectoryPort;
    private final HolidayCalendarPort     holidayCalendarPort;


// SessionScheduleServiceImpl.java (only the bootstrap() method - fully updated)
//
// Assumptions (based on your latest changes):
// 1) BootstrapResponse now has fields:
//    doctors, departments, holidays, templates, seedGlobalTemplateWeekly, seedOverrideWeekly, serverDate
// 2) You created SeedWeeklyScheduleDTO + WeeklySessionDTO/WeeklyDayDTO updated,
//    and BootstrapSeedWeeklyMapper.toSeedWeekly(...)
// 3) Repository methods exist:
//    findByModeAndWeekStartDate(...)
//    findWithChildrenByModeAndWeekStartDate(...)
//    findWithChildrenByModeAndDoctorIdAndWeekStartDate(...)
//
// If any of these names differ in your code, rename accordingly.

    @Override
    @Transactional(readOnly = true)
    public
    BootstrapResponse bootstrap(Long doctorId, LocalDate weekStartISO) {

        LocalDate serverDate = LocalDate.now();

        LocalDate effectiveWeekStart = (weekStartISO == null) ?
                                       serverDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) :
                                       weekStartISO.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        //  seedSchedules (summary list)
        List<SessionScheduleSummaryDTO> seedSchedules; {
            List<SessionSchedule> found; if (doctorId != null) {
                // show all schedules for that doctor & week (GLOBAL_TEMPLATE + OVERRIDE)
                found = sessionScheduleRepository.findByDoctorIdAndWeekStartDate(doctorId, effectiveWeekStart);
            }
            else {
                // bootstrap w/o doctorId -> show doctor overrides for that week (old behavior)
                found = sessionScheduleRepository.findByModeAndWeekStartDate(ScheduleMode.DOCTOR_OVERRIDE,
                                                                             effectiveWeekStart);
            }

            seedSchedules = found.stream().map(SessionScheduleMapper::toSummary).toList();
        }

        // holidays
        List<HolidayDTO> holidays; {
            LocalDate weekEnd = effectiveWeekStart.plusDays(6);
            holidays = holidayCalendarPort.listHolidays(effectiveWeekStart, weekEnd);
        }

        // templates
        List<TemplateLiteDTO> templates =
                sessionScheduleRepository.findByModeAndWeekStartDate(ScheduleMode.GLOBAL_TEMPLATE,
                                                                     effectiveWeekStart).stream().map(s -> BootstrapTemplateMapper.toTemplateLite(s, effectiveWeekStart)).toList();

        // seedGlobalTemplateWeekly + seedOverrideWeekly (your existing logic)
        SeedWeeklyScheduleDTO seedGlobalTemplateWeekly = null; SeedWeeklyScheduleDTO seedOverrideWeekly = null;
        Long                  pickedTemplateId         = null;

        {
            List<SessionSchedule> globals =
                    sessionScheduleRepository.findWithChildrenByModeAndWeekStartDate(ScheduleMode.GLOBAL_TEMPLATE,
                                                                                     effectiveWeekStart);

            SessionSchedule latestGlobal = globals.stream().max(Comparator.comparing(SessionSchedule::getVersion,
                                                                                     Comparator.nullsFirst(Long::compareTo)).thenComparing(SessionSchedule::getId, Comparator.nullsFirst(Long::compareTo))).orElse(null);

            if (latestGlobal != null) {
                pickedTemplateId         = latestGlobal.getId();
                seedGlobalTemplateWeekly = BootstrapSeedWeeklyMapper.toSeedWeekly(latestGlobal, effectiveWeekStart,
                                                                                  pickedTemplateId, null, "Seed " +
                                                                                                          "global " + "schedule" + " for " + "current " + "week", "LOCAL");
            }
        }

        if (doctorId != null) {
            List<SessionSchedule> overrides =
                    sessionScheduleRepository.findWithChildrenByModeAndDoctorIdAndWeekStartDate(ScheduleMode.DOCTOR_OVERRIDE, doctorId, effectiveWeekStart);

            SessionSchedule latestOverride = overrides.stream().max(Comparator.comparing(SessionSchedule::getVersion,
                                                                                         Comparator.nullsFirst(Long::compareTo)).thenComparing(SessionSchedule::getId, Comparator.nullsFirst(Long::compareTo))).orElse(null);

            if (latestOverride != null) {
                seedOverrideWeekly = BootstrapSeedWeeklyMapper.toSeedWeekly(latestOverride, effectiveWeekStart,
                                                                            pickedTemplateId, true, "Seed doctor " +
                                                                                                    "override " +
                                                                                                    "schedule",
                                                                            "OVERRIDDEN");
            }
        }

        return new BootstrapResponse(doctorDirectoryPort.listDoctors(), departmentDirectoryPort.listDepartments(),
                                     holidays, templates, seedSchedules, seedGlobalTemplateWeekly, seedOverrideWeekly
                , serverDate);
    }


    @Override
    @Transactional
    public
    DraftResponse draft(DraftRequest request) {
        // Validate plan using pure validator
        var validateReq = new ValidateRequest(request.mode(), request.doctorId(), request.departmentId(),
                                              request.weekStartDate(), request.slotDurationMin(), request.days());

//        var validation = validationService.validate(validateReq);
//        if (!validation.valid()) {
//            throw SchedulingException.badRequest("SCHEDULE_INVALID",
//                    "Draft validation failed: " + validation.issues().size() + " issue(s)");
//        }

        String actor = actorProvider.currentActor();

        SessionSchedule schedule;
        if (request.scheduleId() == null) {
            // Try to reuse existing schedule for same doctor/week/mode (non-archived) to avoid duplicates
            schedule = sessionScheduleRepository
                    .findByDoctorIdAndWeekStartDateAndModeAndStatusNot(
                            request.doctorId(),
                            request.weekStartDate(),
                            request.mode(),
                            ScheduleStatus.ARCHIVED)
                    .orElse(null);

            if (schedule == null) {
                schedule = SessionSchedule.builder()
                        .mode(request.mode())
                        .doctorId(request.doctorId())
                        .departmentId(request.departmentId())
                        .weekStartDate(request.weekStartDate())
                        .slotDurationMin(request.slotDurationMin())
                        .status(ScheduleStatus.DRAFT)
                        .locked(request.locked())
                        .lockedReason(request.lockedReason())
                        .build();
                schedule.touchForCreate(actor);
            } else {
                // existing schedule found -> require concurrency token even on create path
                if (request.version() == null) {
                    throw SchedulingException.preconditionFailed("VERSION_REQUIRED",
                            "Schedule already exists for this doctor/week. Provide scheduleId with version or If-Match to update.");
                }
                if (!Objects.equals(schedule.getVersion(), request.version())) {
                    throw SchedulingException.preconditionFailed("STALE_VERSION",
                            "Schedule was modified by someone else. Please refresh and retry.");
                }
                if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
                    throw SchedulingException.conflict("SCHEDULE_ARCHIVED",
                            "Cannot edit archived schedule: " + schedule.getId());
                }
                if (schedule.getStatus() == ScheduleStatus.PUBLISHED && schedule.isLocked()) {
                    throw SchedulingException.conflict("SCHEDULE_LOCKED",
                            "Cannot edit locked published schedule: " + schedule.getId());
                }

                schedule.setMode(request.mode());
                schedule.setDoctorId(request.doctorId());
                schedule.setDepartmentId(request.departmentId());
                schedule.setWeekStartDate(request.weekStartDate());
                schedule.setSlotDurationMin(request.slotDurationMin());
                schedule.setLocked(request.locked());
                schedule.setLockedReason(request.lockedReason());
                schedule.touchForUpdate(actor);
            }
        } else {
            schedule =
                    sessionScheduleRepository.findById(request.scheduleId()).orElseThrow(() -> SchedulingException.notFound("SCHEDULE_NOT_FOUND", "Schedule not found: " + request.scheduleId()));

            // Optimistic lock guard before touching JPA @Version
            if (!Objects.equals(schedule.getVersion(), request.version())) {
                throw SchedulingException.conflict("STALE_VERSION",
                                                   "Schedule version mismatch. Expected=" + schedule.getVersion() +
                                                   " provided=" + request.version());
            } if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
                throw SchedulingException.conflict("SCHEDULE_ARCHIVED",
                                                   "Cannot edit archived schedule: " + schedule.getId());
            } if (schedule.getStatus() == ScheduleStatus.PUBLISHED && schedule.isLocked()) {
                throw SchedulingException.conflict("SCHEDULE_LOCKED",
                                                   "Cannot edit locked published schedule: " + schedule.getId());
            }

            schedule.setMode(request.mode()); schedule.setDoctorId(request.doctorId());
            schedule.setDepartmentId(request.departmentId()); schedule.setWeekStartDate(request.weekStartDate());
            schedule.setSlotDurationMin(request.slotDurationMin()); schedule.setLocked(request.locked());
            schedule.setLockedReason(request.lockedReason()); schedule.touchForUpdate(actor);
        }

        // Map plan -> entities
        schedule.setDaysReplace(toEntityDays(schedule, request.days()));

        SessionSchedule saved = sessionScheduleRepository.save(schedule);

        log.info("SessionSchedule draft saved: scheduleId={}, doctorId={}, weekStart={}, status={}, version={}",
                 saved.getId(), saved.getDoctorId(), saved.getWeekStartDate(), saved.getStatus(), saved.getVersion());

        return new DraftResponse(saved.getId(), saved.getVersion(), "Schedule draft saved successfully");
    }

    @Override
    @Transactional
    public
    PublishResponse publish(PublishRequest request) {
        SessionSchedule schedule =
                sessionScheduleRepository.findById(request.scheduleId()).orElseThrow(() -> SchedulingException.notFound("SCHEDULE_NOT_FOUND", "Schedule not found: " + request.scheduleId()));

        if (!Objects.equals(schedule.getVersion(), request.version())) {
            throw SchedulingException.conflict("STALE_VERSION",
                                               "Schedule version mismatch. Expected=" + schedule.getVersion() + " " + "provided=" + request.version());
        }

        if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
            throw SchedulingException.conflict("SCHEDULE_ARCHIVED",
                                               "Cannot publish archived schedule: " + schedule.getId());
        }

        if (schedule.isLocked()) {
            throw SchedulingException.conflict("SCHEDULE_LOCKED", "Schedule is locked: " + schedule.getLockedReason());
        }

        // Re-validate before publish (defensive)
        var validateReq = new ValidateRequest(schedule.getMode(), schedule.getDoctorId(), schedule.getDepartmentId(),
                                              schedule.getWeekStartDate(), schedule.getSlotDurationMin(),
                                              SessionScheduleMapper.toDayPlans(schedule.getDays()));
//        var validation = validationService.validate(validateReq);
//        if (!validation.valid()) {
//            throw SchedulingException.badRequest("SCHEDULE_INVALID",
//                    "Publish validation failed: " + validation.issues().size() + " issue(s)");
//        }

        boolean dryRun       = request.dryRun() || request.slotGenerationMode() == SlotGenerationMode.DRY_RUN;
        boolean failOnBooked = request.failOnBookedConflict();

        String            actor         = actorProvider.currentActor();
        SlotPublishResult publishResult = slotGenerationService.publish(schedule, failOnBooked, dryRun, actor);

        boolean published = !dryRun; if (published) {
            schedule.setStatus(ScheduleStatus.PUBLISHED); schedule.touchForUpdate(actor);
            schedule = sessionScheduleRepository.save(schedule);
        }

        List<PublishConflictDTO> conflicts = publishResult.conflicts() == null ? List.of() :
                                             publishResult.conflicts().stream().map(c -> new PublishConflictDTO(c.slotKey(), c.reason())).toList();

        log.info("SessionSchedule publish: scheduleId={}, doctorId={}, weekStart={}, dryRun={}, planned={}, " +
                 "created={}, updated={}, skipped={}, conflicts={}", schedule.getId(), schedule.getDoctorId(),
                 schedule.getWeekStartDate(), dryRun, publishResult.totalPlanned(), publishResult.created(),
                 publishResult.updated(), publishResult.skipped(), conflicts.size());

        String msg = dryRun ? "Dry run completed. No DB writes performed." : "Published successfully.";

        return new PublishResponse(schedule.getId(), schedule.getVersion(), published, publishResult.totalPlanned(),
                                   publishResult.created(), publishResult.updated(), publishResult.skipped(),
                                   conflicts, msg);
    }

    @Override
    @Transactional
    public CopyFromWeekResponse copyFromWeek(CopyFromWeekRequest request) {
        if (request.mode() == null) {
            throw SchedulingException.badRequest("MODE_REQUIRED", "mode is required");
        }
        if (Objects.equals(request.fromWeekStartISO(), request.toWeekStartISO())) {
            throw SchedulingException.badRequest("COPY_SAME_WEEK", "fromWeekStartISO and toWeekStartISO cannot be same.");
        }
        if (request.mode() == ScheduleMode.DOCTOR_OVERRIDE && request.doctorId() == null) {
            throw SchedulingException.badRequest("DOCTOR_REQUIRED", "doctorId is required when mode=DOCTOR_OVERRIDE.");
        }

        boolean includeBlocks      = Boolean.TRUE.equals(request.includeBlocks());
        boolean includeDayOffFlags = Boolean.TRUE.equals(request.includeDayOffFlags());

        // Resolve source schedule
        SessionSchedule source;
        if (request.mode() == ScheduleMode.DOCTOR_OVERRIDE) {
            source = sessionScheduleRepository
                    .findByDoctorIdAndWeekStartDateAndModeAndStatusNot(request.doctorId(), request.fromWeekStartISO(),
                            ScheduleMode.DOCTOR_OVERRIDE, ScheduleStatus.ARCHIVED)
                    .orElseThrow(() -> SchedulingException.notFound("SOURCE_NOT_FOUND",
                            "Source schedule not found for doctorId=" + request.doctorId() + " weekStart=" + request.fromWeekStartISO()));
        } else { // GLOBAL_TEMPLATE
            if (request.templateId() != null) {
                source = sessionScheduleRepository.findById(request.templateId())
                        .orElseThrow(() -> SchedulingException.notFound("SOURCE_NOT_FOUND",
                                "Template schedule not found: " + request.templateId()));
                if (source.getMode() != ScheduleMode.GLOBAL_TEMPLATE ||
                    !Objects.equals(source.getWeekStartDate(), request.fromWeekStartISO())) {
                    throw SchedulingException.badRequest("SOURCE_MISMATCH",
                            "Template does not match mode=GLOBAL_TEMPLATE and fromWeekStartISO.");
                }
            } else {
                List<SessionSchedule> globals =
                        sessionScheduleRepository.findByModeAndWeekStartDate(ScheduleMode.GLOBAL_TEMPLATE,
                                request.fromWeekStartISO());
                source = globals.stream()
                        .filter(s -> s.getStatus() != ScheduleStatus.ARCHIVED)
                        .max(Comparator.comparing(SessionSchedule::getVersion,
                                Comparator.nullsFirst(Long::compareTo)).thenComparing(SessionSchedule::getId,
                                Comparator.nullsFirst(Long::compareTo)))
                        .orElseThrow(() -> SchedulingException.notFound("SOURCE_NOT_FOUND",
                                "Global template not found for weekStart=" + request.fromWeekStartISO()));
            }
        }

        // Resolve target schedule
        SessionSchedule target;
        if (request.mode() == ScheduleMode.DOCTOR_OVERRIDE) {
            List<SessionSchedule> existing =
                    sessionScheduleRepository.findByDoctorIdAndWeekStartDate(request.doctorId(),
                            request.toWeekStartISO());
            target = existing.stream()
                    .filter(s -> s.getMode() == ScheduleMode.DOCTOR_OVERRIDE && s.getStatus() != ScheduleStatus.ARCHIVED)
                    .findFirst().orElse(null);
        } else { // GLOBAL_TEMPLATE
            List<SessionSchedule> existing =
                    sessionScheduleRepository.findByModeAndWeekStartDate(ScheduleMode.GLOBAL_TEMPLATE,
                            request.toWeekStartISO());
            target = existing.stream().filter(s -> s.getStatus() != ScheduleStatus.ARCHIVED)
                    .max(Comparator.comparing(SessionSchedule::getVersion,
                            Comparator.nullsFirst(Long::compareTo)).thenComparing(SessionSchedule::getId,
                            Comparator.nullsFirst(Long::compareTo)))
                    .orElse(null);
        }

        String actor = actorProvider.currentActor();

        if (target == null) {
            target = SessionSchedule.builder()
                    .mode(request.mode())
                    .doctorId(request.mode() == ScheduleMode.DOCTOR_OVERRIDE ? request.doctorId() : null)
                    .departmentId(source.getDepartmentId())
                    .weekStartDate(request.toWeekStartISO())
                    .slotDurationMin(source.getSlotDurationMin())
                    .status(ScheduleStatus.DRAFT)
                    .locked(false)
                    .build();
            target.touchForCreate(actor);
            target.setDaysReplace(deepCopyDays(target, source.getDays(), includeBlocks, includeDayOffFlags));
            target = sessionScheduleRepository.save(target);

            log.info("CopyFromWeek created: sourceScheduleId={}, targetScheduleId={}, mode={}, doctorId={}, sourceWeek={}, targetWeek={}",
                    source.getId(), target.getId(), request.mode(), request.doctorId(), request.fromWeekStartISO(),
                    request.toWeekStartISO());

                    return new CopyFromWeekResponse(target.getId(), target.getVersion(), true, 0, List.of());
        }

        if (target.getStatus() == ScheduleStatus.PUBLISHED && target.isLocked()) {
            throw SchedulingException.conflict("TARGET_LOCKED", "Target schedule is locked and cannot be modified.");
        }

        int skippedConflicts = 0;
        if (request.strategy() == CopyStrategy.REPLACE_ALL) {
            // Ensure existing days are deleted (orphanRemoval) before inserting new copies to satisfy
            // the unique constraint on (schedule_id, day_of_week).
            if (target.getDays() != null) {
                target.getDays().clear();
                sessionScheduleRepository.saveAndFlush(target);
            }

            target.setSlotDurationMin(source.getSlotDurationMin());
            target.setDepartmentId(source.getDepartmentId());
            target.setStatus(ScheduleStatus.DRAFT);
            target.touchForUpdate(actor);
            target.setDaysReplace(deepCopyDays(target, source.getDays(), includeBlocks, includeDayOffFlags));
            target = sessionScheduleRepository.save(target);

            log.info("CopyFromWeek replaced: sourceScheduleId={}, targetScheduleId={}, mode={}, doctorId={}, targetWeek={}",
                    source.getId(), target.getId(), request.mode(), request.doctorId(), request.toWeekStartISO());
        } else { // MERGE_NON_CONFLICTING
            skippedConflicts = mergeSkipConflicts(target, source, includeBlocks, includeDayOffFlags);
            target.setStatus(ScheduleStatus.DRAFT);
            target.touchForUpdate(actor);
            target = sessionScheduleRepository.save(target);

            log.info("CopyFromWeek merged: sourceScheduleId={}, targetScheduleId={}, mode={}, doctorId={}, targetWeek={}, skippedConflicts={}",
                    source.getId(), target.getId(), request.mode(), request.doctorId(), request.toWeekStartISO(),
                    skippedConflicts);
        }

        List<String> warnings = skippedConflicts > 0
                ? List.of("Skipped " + skippedConflicts + " conflicting interval/block(s).")
                : List.of();

        return new CopyFromWeekResponse(target.getId(), target.getVersion(), true, skippedConflicts, warnings);
    }

    @Override
    @Transactional
    public CopyWeekResponse copyLastWeek(CopyLastWeekRequest request) {
        LocalDate targetWeekStart = (request.targetWeekStartISO() == null ? LocalDate.now() : request.targetWeekStartISO())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sourceWeekStart = targetWeekStart.minusWeeks(1);

        CopyStrategy strategy = request.mergeStrategy() == MergeStrategy.REPLACE
                ? CopyStrategy.REPLACE_ALL
                : CopyStrategy.MERGE_NON_CONFLICTING;

        CopyFromWeekRequest delegated = new CopyFromWeekRequest(
                ScheduleMode.DOCTOR_OVERRIDE,
                request.doctorId(),
                null,
                sourceWeekStart,
                targetWeekStart,
                strategy,
                request.includeBlocks() == null ? Boolean.TRUE : request.includeBlocks(),
                request.includeDayOffFlags() == null ? Boolean.TRUE : request.includeDayOffFlags()
        );

        CopyFromWeekResponse resp = copyFromWeek(delegated);
        return new CopyWeekResponse(resp.scheduleId(), resp.warnings().isEmpty()
                ? "Copy last week completed."
                : String.join("; ", resp.warnings()));
    }

    @Override
    public
    PreviewSlotsResponse previewSlots(PreviewSlotsRequest request) {
        // Validate first
        var validateReq = new ValidateRequest(request.mode(), request.doctorId(), request.departmentId(),
                                              request.weekStartDate(), request.slotDurationMin(), request.days());
        var validation = validationService.validate(validateReq); if (!validation.valid()) {
            throw SchedulingException.badRequest("SCHEDULE_INVALID",
                                                 "Preview validation failed: " + validation.issues().size() + " issue"
                                                 + "(s)");
        }

        // Build a transient schedule (not saved)
        SessionSchedule transientSchedule =
                SessionSchedule.builder().id(-1L).mode(request.mode()).doctorId(request.doctorId()).departmentId(request.departmentId()).weekStartDate(request.weekStartDate()).slotDurationMin(request.slotDurationMin()).status(ScheduleStatus.DRAFT).locked(false).build();

        transientSchedule.setDaysReplace(toEntityDays(transientSchedule, request.days()));
        return slotGenerationService.preview(transientSchedule);
    }

    @Override
    public
    SessionScheduleDetailDTO getById(Long scheduleId) {
        SessionSchedule schedule =
                sessionScheduleRepository.findById(scheduleId).orElseThrow(() -> SchedulingException.notFound(
                        "SCHEDULE_NOT_FOUND", "Schedule not " + "found: " + scheduleId));
        return SessionScheduleMapper.toDetail(schedule);
    }

    @Override
    public
    SessionScheduleVersionDTO getVersion(Long scheduleId) {
        SessionSchedule schedule =
                sessionScheduleRepository.findById(scheduleId).orElseThrow(() -> SchedulingException.notFound(
                        "SCHEDULE_NOT_FOUND", "Schedule not " + "found: " + scheduleId));
        return new SessionScheduleVersionDTO(schedule.getVersion());
    }


    @Override
    public
    SearchResponse search(ScheduleMode mode, Long doctorId, LocalDate weekStartISO) {
        if (mode == null) {
            throw SchedulingException.badRequest("MODE_REQUIRED", "mode is required");
        } if (weekStartISO == null) {
            throw SchedulingException.badRequest("WEEK_START_REQUIRED", "weekStartISO is required");
        }

        List<SessionSchedule> found; if (doctorId != null) {
            found = sessionScheduleRepository.findByModeAndDoctorIdAndWeekStartDate(mode, doctorId, weekStartISO);
        }
        else {
            found = sessionScheduleRepository.findByModeAndWeekStartDate(mode, weekStartISO);
        }

        List<SessionScheduleSummaryDTO> items = found.stream().map(SessionScheduleMapper::toSummary).toList();

        return new SearchResponse(items);
    }

    @Override
    @Transactional
    public
    ArchiveResponse archive(Long scheduleId, Long version) {
        SessionSchedule schedule =
                sessionScheduleRepository.findById(scheduleId).orElseThrow(() -> SchedulingException.notFound(
                        "SCHEDULE_NOT_FOUND", "Schedule not " + "found: " + scheduleId));

        if (!Objects.equals(schedule.getVersion(), version)) {
            throw SchedulingException.conflict("STALE_VERSION",
                                               "Schedule version mismatch. Expected=" + schedule.getVersion() + " " + "provided=" + version);
        } if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
            return new ArchiveResponse(schedule.getId(), schedule.getVersion(), "Already archived.");
        }

        String actor = actorProvider.currentActor(); schedule.setStatus(ScheduleStatus.ARCHIVED);
        schedule.touchForUpdate(actor); schedule = sessionScheduleRepository.save(schedule);

        log.info("SessionSchedule archived: scheduleId={}, doctorId={}, weekStart={}, version={}", schedule.getId(),
                 schedule.getDoctorId(), schedule.getWeekStartDate(), schedule.getVersion());

        return new ArchiveResponse(schedule.getId(), schedule.getVersion(), "Archived successfully.");
    }

    // -----------------------
    // Helpers: mapping + merge
    // -----------------------

    private List<SessionScheduleDay> deepCopyDays(SessionSchedule targetSchedule, List<SessionScheduleDay> sourceDays,
                                                  boolean includeBlocks, boolean includeDayOffFlags) {
        List<SessionScheduleDay> out = new ArrayList<>();
        for (SessionScheduleDay sd : sourceDays) {
            SessionScheduleDay td = SessionScheduleDay.builder()
                    .schedule(targetSchedule)
                    .dayOfWeek(sd.getDayOfWeek())
                    .dayOff(includeDayOffFlags && sd.isDayOff())
                    .build();

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

            if (includeBlocks) {
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
            } else {
                td.setBlocksReplace(List.of());
            }

            out.add(td);
        }
        return out;
    }

    private int mergeSkipConflicts(SessionSchedule target, SessionSchedule source, boolean includeBlocks,
                                   boolean includeDayOffFlags) {
        if (target.getDays() == null) {
            target.setDaysReplace(new ArrayList<>());
        }
        Map<DayOfWeek, SessionScheduleDay> targetMap = target.getDays().stream()
                .collect(Collectors.toMap(SessionScheduleDay::getDayOfWeek, d -> d, (a, b) -> a, LinkedHashMap::new));

        int skipped = 0;

        for (SessionScheduleDay sourceDay : source.getDays()) {
            SessionScheduleDay targetDay = targetMap.get(sourceDay.getDayOfWeek());
            if (targetDay == null) {
                SessionScheduleDay copied = deepCopyDays(target, List.of(sourceDay), includeBlocks, includeDayOffFlags).get(0);
                target.getDays().add(copied);
                targetMap.put(copied.getDayOfWeek(), copied);
                continue;
            }

            if (includeDayOffFlags) {
                targetDay.setDayOff(sourceDay.isDayOff());
            }
            if (targetDay.isDayOff()) continue;

            // Merge intervals
            for (SessionScheduleInterval si : sourceDay.getIntervals()) {
                boolean overlaps = targetDay.getIntervals().stream()
                        .anyMatch(ti -> si.getStartTime().isBefore(ti.getEndTime()) && ti.getStartTime().isBefore(si.getEndTime()));
                if (overlaps) {
                    skipped++;
                } else {
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
            if (includeBlocks) {
                if (targetDay.getBlocks() == null) {
                    targetDay.setBlocksReplace(new ArrayList<>());
                }
                for (SessionScheduleBlock sb : sourceDay.getBlocks()) {
                    boolean overlaps = targetDay.getBlocks().stream()
                            .anyMatch(tb -> sb.getStartTime().isBefore(tb.getEndTime()) && tb.getStartTime().isBefore(sb.getEndTime()));
                    if (overlaps) {
                        skipped++;
                    } else {
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
        }
        return skipped;
    }


    private
    List<SessionScheduleDay> toEntityDays(SessionSchedule schedule, List<SessionScheduleDayPlanDTO> plans) {
        if (plans == null) return List.of();

        // Map existing days by DayOfWeek (so we reuse same DB row)
        Map<DayOfWeek, SessionScheduleDay> existingByDow = schedule.getDays() == null ? new LinkedHashMap<>() :
                                                           schedule.getDays().stream().filter(d -> d.getDayOfWeek() != null).collect(Collectors.toMap(SessionScheduleDay::getDayOfWeek, d -> d, (a, b) -> a, LinkedHashMap::new));

        List<SessionScheduleDay> out = new ArrayList<>();

        for (SessionScheduleDayPlanDTO p : plans) {
            DayOfWeek dow = p.dayOfWeek();

            // Reuse if exists, else create new
            SessionScheduleDay d = existingByDow.getOrDefault(dow,
                                                              SessionScheduleDay.builder().schedule(schedule).dayOfWeek(dow).build());

            // Update fields
            d.setDayOff(p.dayOff());

            // Replace children safely (these methods should clear+add and set parent)
            d.setIntervalsReplace(toIntervals(d, p.intervals())); d.setBlocksReplace(toBlocks(d, p.blocks()));

            out.add(d);
        }

        return out;
    }

    private
    List<SessionScheduleInterval> toIntervals(SessionScheduleDay day, List<SessionScheduleIntervalDTO> intervals) {
        if (intervals == null) return List.of();
        Map<Long, SessionScheduleInterval> existingById = day.getIntervals() == null ? Map.of() :
                                                          day.getIntervals().stream().filter(i -> i.getId() != null).collect(Collectors.toMap(SessionScheduleInterval::getId, i -> i, (a, b) -> a, LinkedHashMap::new));
        Map<String, SessionScheduleInterval> existingByKey = day.getIntervals() == null ? Map.of() :
                                                             day.getIntervals().stream().collect(Collectors.toMap(i -> intervalKey(i.getStartTime(), i.getEndTime(), i.getSessionType()), i -> i, (a, b) -> a, LinkedHashMap::new));

        List<SessionScheduleInterval> out = new ArrayList<>(); for (SessionScheduleIntervalDTO it : intervals) {
            SessionScheduleInterval entity = it.id() != null ? existingById.get(it.id()) : null; if (entity == null) {
                entity = existingByKey.get(intervalKey(it.startTime(), it.endTime(), it.sessionType()));
            } if (entity == null) {
                entity = SessionScheduleInterval.builder().day(day).build();
            } entity.setDay(day); // ensure association in case of reuse
            entity.setStartTime(it.startTime()); entity.setEndTime(it.endTime());
            entity.setSessionType(it.sessionType()); entity.setCapacity(it.capacity()); out.add(entity);
        } return out;
    }

    private
    List<SessionScheduleBlock> toBlocks(SessionScheduleDay day, List<SessionScheduleBlockDTO> blocks) {
        if (blocks == null) return List.of();
        Map<Long, SessionScheduleBlock> existingById = day.getBlocks() == null ? Map.of() :
                                                       day.getBlocks().stream().filter(b -> b.getId() != null).collect(Collectors.toMap(SessionScheduleBlock::getId, b -> b, (a, c) -> a, LinkedHashMap::new));
        Map<String, SessionScheduleBlock> existingByKey = day.getBlocks() == null ? Map.of() :
                                                          day.getBlocks().stream().collect(Collectors.toMap(b -> blockKey(b.getBlockType(), b.getStartTime(), b.getEndTime()), b -> b, (a, c) -> a, LinkedHashMap::new));

        List<SessionScheduleBlock> out = new ArrayList<>(); for (SessionScheduleBlockDTO b : blocks) {
            SessionScheduleBlock entity = b.id() != null ? existingById.get(b.id()) : null; if (entity == null) {
                entity = existingByKey.get(blockKey(b.blockType(), b.startTime(), b.endTime()));
            } if (entity == null) {
                entity = SessionScheduleBlock.builder().day(day).build();
            } entity.setDay(day); // ensure association in case of reuse
            entity.setBlockType(b.blockType()); entity.setStartTime(b.startTime()); entity.setEndTime(b.endTime());
            entity.setReason(b.reason()); out.add(entity);
        } return out;
    }


    private
    String intervalKey(java.time.LocalTime start, java.time.LocalTime end,
                       com.MediHubAPI.model.enums.SessionType type) {
        return start + "|" + end + "|" + type;
    }

    private
    String blockKey(com.MediHubAPI.model.enums.BlockType type, java.time.LocalTime start, java.time.LocalTime end) {
        return type + "|" + start + "|" + end;
    }
}
