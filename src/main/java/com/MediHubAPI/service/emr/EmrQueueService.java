package com.MediHubAPI.service.emr;

import com.MediHubAPI.dto.emr.EmrQueueItemDto;
import com.MediHubAPI.repository.EmrQueueRepository;
import com.MediHubAPI.repository.projection.EmrQueueRowProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmrQueueService {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EmrQueueRepository emrQueueRepository;

    public List<EmrQueueItemDto> getQueue(LocalDate date, Long doctorId) {

        List<EmrQueueRowProjection> rows = emrQueueRepository.fetchEmrQueue(date, doctorId);

        List<EmrQueueItemDto> out = new ArrayList<>(rows.size());
        int rowId = 1;

        for (EmrQueueRowProjection r : rows) {

            String ageSexLabel = buildAgeSexLabel(r.getSex(), r.getDob());
            String uiStatus = mapToUiStatus(r.getAppointmentStatus());

            boolean hasInsurance = containsIgnoreCase(r.getInvoiceQueue(), "INSUR")
                    || containsIgnoreCase(r.getInvoiceNotes(), "INSUR");

            boolean hasReferrer =
                    hasText(r.getReferrerName()) ||
                            hasText(r.getReferrerNumber()) ||
                            hasText(r.getReferrerType());

            String createdAtISO = (r.getCreatedAt() == null) ? null : r.getCreatedAt().format(ISO_LOCAL);
            String slotTime = (r.getSlotTime() == null) ? null : r.getSlotTime().toString();   // "HH:mm:ss"
            String visitDate = (r.getVisitDate() == null) ? null : r.getVisitDate().toString(); // "yyyy-MM-dd"

            out.add(new EmrQueueItemDto(
                    rowId++,              // 1 rowId
                    r.getTokenNo(),       // 2 tokenNo

                    r.getDoctorId(),      // 3 doctorId
                    r.getAppointmentId(), // 4 appointmentId
                    slotTime,             // 5 slotTime
                    visitDate,            // 6 visitDate

                    r.getPatientId(),     // 7 patientId
                    r.getPatientName(),   // 8 patientName
                    ageSexLabel,          // 9 ageSexLabel
                    r.getDoctorName(),    // 10 doctorName
                    createdAtISO,         // 11 createdAtISO
                    "",                   // 12 alerts
                    uiStatus,             // 13 status
                    hasInsurance,         // 14 hasInsurance
                    hasReferrer           // 15 hasReferrer
            ));
        }

        log.info("EMR queue fetched: date={}, doctorId={}, count={}", date, doctorId, out.size());
        return out;
    }

    private String mapToUiStatus(String appointmentStatus) {
        if (appointmentStatus == null) return "Reserved";
        return switch (appointmentStatus) {
            case "BOOKED" -> "Reserved";
            case "ARRIVED" -> "Waiting";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            case "NO_SHOW" -> "No Show";
            default -> appointmentStatus;
        };
    }

    private String buildAgeSexLabel(String sex, LocalDate dob) {
        String sexLabel = "Other";
        if (sex != null) {
            if ("MALE".equalsIgnoreCase(sex)) sexLabel = "Male";
            else if ("FEMALE".equalsIgnoreCase(sex)) sexLabel = "Female";
        }

        if (dob == null) {
            return sexLabel + " | (NA)";
        }

        int years = Period.between(dob, LocalDate.now()).getYears();
        if (years < 0) years = 0;

        // Your required format example: "Female | (35 Y)"
        return sexLabel + " | (" + years + " Y)";
    }

    private boolean containsIgnoreCase(String text, String needle) {
        return text != null && needle != null && text.toLowerCase().contains(needle.toLowerCase());
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
