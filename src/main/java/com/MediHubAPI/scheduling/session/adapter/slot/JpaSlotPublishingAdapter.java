package com.MediHubAPI.scheduling.session.adapter.slot;

import com.MediHubAPI.model.Slot;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.model.enums.SlotType;
import com.MediHubAPI.repository.SlotRepository;
import com.MediHubAPI.service.scheduling.session.port.SlotPublishingPort;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishCommand;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Production adapter: SessionSchedule -> Slot rows
 *
 * Hard rules (HMS-safe):
 *  - Idempotent by (doctorId, date, startTime, endTime)
 *  - NEVER delete anything here
 *  - NEVER modify a slot if:
 *      - slot.getAppointment() != null OR
 *      - slot.getStatus() == BOOKED
 *  - If schedule wants to BLOCK a BOOKED slot:
 *      - if failOnBookedConflict=true => conflict
 *      - else => skip
 *
 * Mapping:
 *  - interval slot => SlotStatus.AVAILABLE, SlotType.REGULAR
 *  - blocked slot  => SlotStatus.BLOCKED (default) or SlotStatus.LUNCH_BREAK when blockType==LUNCH
 *    (SlotType remains REGULAR to avoid breaking booking flow assumptions)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaSlotPublishingAdapter implements SlotPublishingPort {

    private final SlotRepository slotRepository;

    @Override
    @Transactional
    public SlotPublishResult publishSlots(SlotPublishCommand command) {

        Objects.requireNonNull(command, "command");
        if (command.doctorId() == null) {
            return new SlotPublishResult(
                    command.desiredSlots().size(),
                    0,
                    0,
                    command.desiredSlots().size(),
                    command.desiredSlots().stream()
                            .map(s -> new SlotPublishResult.Conflict(s.slotKey(), "NO_DOCTOR_TARGET"))
                            .toList()
            );
        }

        final Long doctorId = command.doctorId();
        final String actor = command.actor() == null ? "SYSTEM" : command.actor();

        // Prefetch all slots for each date once, then operate in-memory.
        // Keyed by (date|start|end).
        Map<String, Slot> existingByKey = new HashMap<>();

        Set<LocalDate> dates = new HashSet<>();
        for (SlotPublishCommand.DesiredSlot ds : command.desiredSlots()) {
            dates.add(ds.date());
        }

        for (LocalDate date : dates) {
            List<Slot> existing = slotRepository.findByDoctorIdAndDate(doctorId, date);
            for (Slot s : existing) {
                existingByKey.put(key(date, s.getStartTime(), s.getEndTime()), s);
            }
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<SlotPublishResult.Conflict> conflicts = new ArrayList<>();

        for (SlotPublishCommand.DesiredSlot desired : command.desiredSlots()) {
            LocalDate date = desired.date();
            LocalTime start = desired.startTime();
            LocalTime end = desired.endTime();
            String slotKey = desired.slotKey() != null ? desired.slotKey() : key(date, start, end);

            SlotStatus desiredStatus = mapDesiredStatus(desired);
            SlotType desiredType = SlotType.REGULAR; // keep stable unless you explicitly want otherwise
            boolean desiredRecurring = true;          // schedule-published slots are recurring by definition here

            Slot existing = existingByKey.get(key(date, start, end));

            if (existing == null) {
                // Idempotency extra-guard:
                // If another transaction created it just now, existsBy... catches it.
                boolean exists = slotRepository.existsByDoctorIdAndDateAndStartTimeAndEndTime(
                        doctorId, date, start, end
                );
                if (exists) {
                    // Reload for consistent behavior (best-effort): find in the already-prefetched map is not possible.
                    // We'll skip safely.
                    skipped++;
                    conflicts.add(new SlotPublishResult.Conflict(slotKey, "RACE_EXISTS_SKIP"));
                    continue;
                }

                // Create new Slot
                Slot toCreate = Slot.builder()
                        .doctor(User.builder().id(doctorId).build()) // uses existing User entity; no DB fetch required
                        .date(date)
                        .startTime(start)
                        .endTime(end)
                        .status(desiredStatus)
                        .type(desiredType)
                        .recurring(desiredRecurring)
                        .createdBy(actor)
                        .updatedBy(actor)
                        .notes(buildNotes(command.scheduleId(), desired))
                        .build();

                Slot saved = slotRepository.save(toCreate);
                existingByKey.put(key(date, start, end), saved);

                created++;
                continue;
            }

            // Existing slot present: apply "safe update" rules.
            if (isBookedOrHasAppointment(existing)) {
                // schedule wants to block but slot is booked => conflict/skip
                if (desired.blocked()) {
                    String reason = "BOOKED_CONFLICT";
                    conflicts.add(new SlotPublishResult.Conflict(slotKey, reason));
                    skipped++;
                    if (command.failOnBookedConflict()) {
                        // fail fast: you asked for strict conflict behavior
                        throw new SlotPublishBookedConflictException(
                                "Cannot block a booked slot: " + slotKey + " (doctorId=" + doctorId + ")"
                        );
                    }
                } else {
                    // schedule wants AVAILABLE but it's booked -> we do nothing (booking flow owns this)
                    skipped++;
                }
                continue;
            }

            // Not booked and no appointment: we can update status/type/notes safely.
            boolean changed = false;

            if (existing.getStatus() != desiredStatus) {
                existing.setStatus(desiredStatus);
                changed = true;
            }

            if (existing.getType() != desiredType) {
                existing.setType(desiredType);
                changed = true;
            }

            if (existing.isRecurring() != desiredRecurring) {
                existing.setRecurring(desiredRecurring);
                changed = true;
            }

            // Audit + notes
            String newNotes = buildNotes(command.scheduleId(), desired);
            if (!Objects.equals(existing.getNotes(), newNotes)) {
                existing.setNotes(newNotes);
                changed = true;
            }

            if (!Objects.equals(existing.getUpdatedBy(), actor)) {
                existing.setUpdatedBy(actor);
                changed = true;
            }

            if (changed) {
                slotRepository.save(existing);
                updated++;
            } else {
                skipped++;
            }
        }

        int totalPlanned = command.desiredSlots().size();
        log.info("Slot publish completed: scheduleId={}, doctorId={}, weekStart={}, planned={}, created={}, updated={}, skipped={}, conflicts={}",
                command.scheduleId(), doctorId, command.weekStartDate(), totalPlanned, created, updated, skipped, conflicts.size());

        return new SlotPublishResult(totalPlanned, created, updated, skipped, conflicts);
    }

    private boolean isBookedOrHasAppointment(Slot slot) {
        // Hard safety: appointment relation is authoritative
        if (slot.getAppointment() != null) return true;
        // Extra safety: BOOKED status must be treated as immutable
        return slot.getStatus() == SlotStatus.BOOKED;
    }

    private SlotStatus mapDesiredStatus(SlotPublishCommand.DesiredSlot desired) {
        if (!desired.blocked()) {
            return SlotStatus.AVAILABLE;
        }
        // Use LUNCH_BREAK if LUNCH block
        if (desired.blockType() != null && desired.blockType().name().equals("LUNCH")) {
            return SlotStatus.LUNCH_BREAK;
        }
        return SlotStatus.BLOCKED;
    }

    private String buildNotes(Long scheduleId, SlotPublishCommand.DesiredSlot desired) {
        // Notes are safe, non-breaking debugging metadata
        StringBuilder sb = new StringBuilder();
        sb.append("sessionScheduleId=").append(scheduleId);
        sb.append("; slotKey=").append(desired.slotKey());
        sb.append("; blocked=").append(desired.blocked());
        if (desired.blockType() != null) sb.append("; blockType=").append(desired.blockType());
        if (desired.sessionType() != null) sb.append("; sessionType=").append(desired.sessionType());
        if (desired.capacity() != null) sb.append("; capacity=").append(desired.capacity());
        return sb.toString();
    }

    private String key(LocalDate date, LocalTime start, LocalTime end) {
        return date + "|" + start + "|" + end;
    }

    /**
     * Local runtime exception to trigger rollback when strict booked conflict is requested.
     * This is intentionally in adapter (write boundary) to ensure transactional rollback.
     */
    public static class SlotPublishBookedConflictException extends RuntimeException {
        public SlotPublishBookedConflictException(String message) {
            super(message);
        }
    }
}
