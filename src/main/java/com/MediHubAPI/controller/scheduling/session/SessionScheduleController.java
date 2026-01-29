package com.MediHubAPI.controller.scheduling.session;

import com.MediHubAPI.dto.scheduling.session.archive.ArchiveResponse;
import com.MediHubAPI.dto.scheduling.session.bootstrap.BootstrapResponse;
import com.MediHubAPI.dto.scheduling.session.draft.DraftRequest;
import com.MediHubAPI.dto.scheduling.session.draft.DraftResponse;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleDetailDTO;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleVersionDTO;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsRequest;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsResponse;
import com.MediHubAPI.dto.scheduling.session.publish.PublishRequest;
import com.MediHubAPI.dto.scheduling.session.publish.PublishResponse;
import com.MediHubAPI.dto.scheduling.session.search.SearchResponse;
import com.MediHubAPI.dto.scheduling.session.validate.ValidateRequest;
import com.MediHubAPI.dto.scheduling.session.validate.ValidateResponse;
import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.service.scheduling.session.port.SessionScheduleService;
import com.MediHubAPI.service.scheduling.session.port.ValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scheduling/session-schedules")
public class SessionScheduleController {

    private final SessionScheduleService sessionScheduleService;
    private final ValidationService validationService;


    @GetMapping("/bootstrap")
    public BootstrapResponse bootstrap(
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "weekStartISO", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate weekStartISO
    ) {
        return sessionScheduleService.bootstrap(doctorId, weekStartISO);
    }


    @PostMapping("/validate")
    public ValidateResponse validate(@Valid @RequestBody ValidateRequest request) {
        return validationService.validate(request);
    }


    @PostMapping("/draft")
    public DraftResponse draft(@Valid @RequestBody DraftRequest request) {
        return sessionScheduleService.draft(request);
    }


    @PostMapping("/publish")
    public PublishResponse publish(@Valid @RequestBody PublishRequest request) {
        return sessionScheduleService.publish(request);
    }


    @PostMapping("/preview-slots")
    public PreviewSlotsResponse previewSlots(@Valid @RequestBody PreviewSlotsRequest request) {
        return sessionScheduleService.previewSlots(request);
    }


    @GetMapping("/{scheduleId}")
    public SessionScheduleDetailDTO getById(@PathVariable Long scheduleId) {
        return sessionScheduleService.getById(scheduleId);
    }

    @GetMapping("/{scheduleId}/version")
    public SessionScheduleVersionDTO getVersion(@PathVariable Long scheduleId) {
        return sessionScheduleService.getVersion(scheduleId);
    }


    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(name = "mode") ScheduleMode mode,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "weekStartISO") @DateTimeFormat(iso = ISO.DATE) LocalDate weekStartISO
    ) {
        return sessionScheduleService.search(mode, doctorId, weekStartISO);
    }


    @PostMapping("/{scheduleId}/archive")
    public ArchiveResponse archive(
            @PathVariable Long scheduleId,
            @RequestParam(name = "version") Long version
    ) {
        return sessionScheduleService.archive(scheduleId, version);
    }
}
