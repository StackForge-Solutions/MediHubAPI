package com.MediHubAPI.dto;

import com.MediHubAPI.model.enums.InitiatorRole;
import com.MediHubAPI.model.enums.ShiftDirection;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftAppointmentsResult {

    // Core counters
    private int totalMatched;
    private int totalShifted;
    private int totalSkipped;

    // IDs for quick UI highlighting
    private List<Long> shiftedIds;
    private List<Long> skippedIds;

    // Human-friendly message
    private String message;

    // Per-slot and aggregated skip telemetry
    // appointmentId/slotId -> reason
    private Map<Long, String> skippedReasons;
    // reason -> count
    private Map<String, Integer> skipReasonCounts;

    //  Context (helps reproduce/debug later)
    private Long doctorId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startingFrom;

    // Effective signed minutes applied (POSTPONE => +, ADVANCE => -)
    private int deltaMinutes;

    private ShiftDirection direction;
    private boolean dryRun;

    private Long initiatedByUserId;
    private InitiatorRole initiatedByRole;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    // ---- Convenience factory (optional) ----
    public static ShiftAppointmentsResult ofSuccess(Long doctorId,
                                                    LocalDate date,
                                                    LocalTime startingFrom,
                                                    int deltaMinutes,
                                                    ShiftDirection direction,
                                                    boolean dryRun,
                                                    Long initiatedByUserId,
                                                    InitiatorRole initiatedByRole,
                                                    int matched,
                                                    int shifted,
                                                    int skipped,
                                                    List<Long> shiftedIds,
                                                    List<Long> skippedIds,
                                                    Map<Long, String> skippedReasons,
                                                    Map<String, Integer> skipReasonCounts) {

        String msg = (dryRun ? "[DRY-RUN] " : "") +
                "Attempted to " + direction.name().toLowerCase() + " " + matched + " slot(s) by " +
                Math.abs(deltaMinutes) + " minutes from " + startingFrom +
                " on " + date + ". Shifted=" + shifted + ", Skipped=" + skipped + ".";

        return ShiftAppointmentsResult.builder()
                .doctorId(doctorId)
                .date(date)
                .startingFrom(startingFrom)
                .deltaMinutes(deltaMinutes)
                .direction(direction)
                .dryRun(dryRun)
                .initiatedByUserId(initiatedByUserId)
                .initiatedByRole(initiatedByRole)
                .totalMatched(matched)
                .totalShifted(shifted)
                .totalSkipped(skipped)
                .shiftedIds(shiftedIds)
                .skippedIds(skippedIds)
                .skippedReasons(skippedReasons)
                .skipReasonCounts(skipReasonCounts)
                .message(msg)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
