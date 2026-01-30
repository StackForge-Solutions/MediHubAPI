package com.MediHubAPI.service.scheduling.session.port;


import com.MediHubAPI.dto.scheduling.session.archive.ArchiveResponse;
import com.MediHubAPI.dto.scheduling.session.bootstrap.BootstrapResponse;
import com.MediHubAPI.dto.scheduling.session.draft.DraftRequest;
import com.MediHubAPI.dto.scheduling.session.draft.DraftResponse;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleDetailDTO;
import com.MediHubAPI.dto.scheduling.session.get.SessionScheduleVersionDTO;
import com.MediHubAPI.dto.scheduling.session.copy.CopyFromWeekRequest;
import com.MediHubAPI.dto.scheduling.session.copy.CopyFromWeekResponse;
import com.MediHubAPI.dto.scheduling.session.copy.CopyLastWeekRequest;
import com.MediHubAPI.dto.scheduling.session.copy.CopyWeekResponse;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsRequest;
import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsResponse;
import com.MediHubAPI.dto.scheduling.session.publish.PublishRequest;
import com.MediHubAPI.dto.scheduling.session.publish.PublishResponse;
import com.MediHubAPI.dto.scheduling.session.search.SearchResponse;
import com.MediHubAPI.model.enums.ScheduleMode;

import java.time.LocalDate;

public interface SessionScheduleService {


    BootstrapResponse bootstrap(Long doctorId, LocalDate weekStartISO);

    DraftResponse draft(DraftRequest request);

    PublishResponse publish(PublishRequest request);

    CopyFromWeekResponse copyFromWeek(CopyFromWeekRequest request);
    CopyWeekResponse copyLastWeek(CopyLastWeekRequest request);


    PreviewSlotsResponse previewSlots(PreviewSlotsRequest request);

    SessionScheduleDetailDTO getById(Long scheduleId);

    SessionScheduleVersionDTO getVersion(Long scheduleId);

    SearchResponse search(ScheduleMode mode, Long doctorId, LocalDate weekStartISO);

    ArchiveResponse archive(Long scheduleId, Long version);
}
