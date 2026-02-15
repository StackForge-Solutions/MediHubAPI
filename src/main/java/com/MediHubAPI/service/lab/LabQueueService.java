package com.MediHubAPI.service.lab;

import com.MediHubAPI.dto.lab.LabQueueItemDto;
import com.MediHubAPI.repository.LabQueueRepository;
import com.MediHubAPI.repository.projection.LabQueueRowProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabQueueService {

    private final LabQueueRepository labQueueRepository;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public Page<LabQueueItemDto> fetchLabQueuePage(LocalDate date, String status, String search, String room, int page, int pageSize) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(pageSize, 1), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        String normalizedStatus = normalizeStatus(status);
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        String normalizedRoom = (room == null || room.isBlank()) ? null : room.trim();

        Page<LabQueueRowProjection> rows = labQueueRepository.fetchQueue(date, normalizedStatus, normalizedSearch, normalizedRoom, pageable);

        return rows.map(r -> LabQueueItemDto.builder()
                .token(r.getToken())
                .patientId(r.getPatientId())
                .patientName(r.getPatientName())
                .ageLabel(r.getAgeLabel())
                .phone(r.getPhone())
                .doctorName(r.getDoctorName())
                .referrerName(r.getReferrerName())
                .createdAtLabel(r.getCreatedAt() == null ? null : r.getCreatedAt().format(DT))
                .dateISO(r.getDateISO())
                .status(toClientStatus(r.getStatus()))
                .insurance(toBool(r.getInsurance()))
                .referrer(toBool(r.getReferrer()))
                .notes(r.getNotes())
                .room(r.getRoom())
                .build());
    }

    private String toClientStatus(String status) {
        if (status == null) return null;
        return status.replace("_", "").toLowerCase();
    }

    private Boolean toBool(Integer val) {
        if (val == null) return null;
        return val != 0;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "all";
        String s = status.trim().toUpperCase();
        if ("NOSHOW".equals(s)) return "NO_SHOW";
        return s;
    }
}
