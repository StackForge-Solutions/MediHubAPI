package com.MediHubAPI.service.scheduling.session.port;


import com.MediHubAPI.dto.scheduling.session.archive.ArchiveResponse;
import com.MediHubAPI.dto.scheduling.session.bootstrap.BootstrapResponse;
import com.MediHubAPI.dto.scheduling.session.copy.CopyWeekRequest;
import com.MediHubAPI.dto.scheduling.session.copy.CopyWeekResponse;
import com.MediHubAPI.dto.scheduling.session.draft.DraftRequest;
import com.MediHubAPI.dto.scheduling.session.draft.DraftResponse;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleDetailDTO;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsRequest;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsResponse;
import com.MediHubAPI.dto.scheduling.session.publish.PublishRequest;
import com.MediHubAPI.dto.scheduling.session.publish.PublishResponse;
import com.MediHubAPI.dto.scheduling.session.search.SearchResponse;
import com.MediHubAPI.model.enums.ScheduleMode;

import java.time.LocalDate;

public interface SessionScheduleService {


    BootstrapResponse bootstrapSessionSchedules(Long doctorId, LocalDate weekStartISO);

    DraftResponse saveDraftSchedule(DraftRequest request);

    PublishResponse publishSchedule(PublishRequest request);

    CopyWeekResponse copyWeekSchedule(CopyWeekRequest request);

    PreviewSlotsResponse previewScheduleSlots(PreviewSlotsRequest request);

    SessionScheduleDetailDTO getScheduleById(Long scheduleId);

    SearchResponse searchSchedules(ScheduleMode mode, Long doctorId, LocalDate weekStartISO);

    ArchiveResponse archiveSchedule(Long scheduleId, Long version);
}
