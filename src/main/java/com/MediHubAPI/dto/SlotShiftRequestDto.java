package com.MediHubAPI.dto;

import com.MediHubAPI.model.enums.InitiatorRole;
import com.MediHubAPI.model.enums.ShiftDirection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SlotShiftRequestDto {

    @NotNull
    private LocalDate date;                 // Which day to operate on

    // Parameter One: “By 0 Days 0 Hours 0 Minutes”
    private int shiftByDays;                // can be negative
    private int shiftByHours;               // can be negative
    private int shiftByMinutes;             // can be negative

    // Parameter Two: “Appointments Starting From: 10:00 AM/PM”
    @NotNull
    private LocalTime startingFrom;         // inclusive

    // Parameter Three: “Send SMS to patients”
    private boolean sendSms;                // default false

    // Parameter Four: “Reason for shifting”
    @Size(max = 500)
    private String reason;                  // optional but recommended

    // ✅ NEW: explicit intent + audit
    @NotNull
    private ShiftDirection direction;       // ADVANCE (earlier) or POSTPONE (later)

    @NotNull
    private Long initiatedByUserId;         // who clicked “Shift”

    @NotNull
    private InitiatorRole initiatedByRole;  // only DOCTOR or STAFF

    // ✅ NEW: scope/behavior knobs
    private boolean onlyBooked;             // true = shift only booked (has appointment)
    private boolean includeBlocked;         // include BLOCKED slots in shift window?
    private boolean dryRun;                 // true = calculate/return result but DON’T save

    /** Raw absolute minutes entered in UI (can be pos/neg; we normalize with direction). */
    public int totalShiftMinutes() {
        long total = (long) shiftByDays * 24 * 60
                + (long) shiftByHours * 60
                + (long) shiftByMinutes;
        if (total > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (total < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) total;
    }

    /** Signed minutes to apply, respecting direction explicitly. */
    public int effectiveShiftMinutes() {
        int abs = Math.abs(totalShiftMinutes());
        return direction == ShiftDirection.POSTPONE ? +abs : -abs; // ADVANCE = earlier = negative minutes
    }
}
