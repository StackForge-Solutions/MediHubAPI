package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.Slot;
import com.MediHubAPI.model.enums.AppointmentStatus;
import com.MediHubAPI.model.enums.AppointmentType;
import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.SlotRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.SlotService;
import com.MediHubAPI.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final SmsService smsService;


    @Transactional
    public ShiftAppointmentsResult shiftSlots(Long doctorId, SlotShiftRequestDto request) {
        // 1) Role gate (only DOCTOR or STAFF)
        switch (request.getInitiatedByRole()) {
            case DOCTOR, STAFF -> {} // allowed
            default -> throw new HospitalAPIException(HttpStatus.FORBIDDEN, "Only doctor or staff can shift appointments");
        }

        // 2) Validate user exists (auditable initiator)
        userRepository.findById(request.getInitiatedByUserId())
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "Initiator not found"));

        // 3) Load slots for the date, filter by startingFrom + scope flags
        // ... (role checks + load all)
        List<Slot> all = slotRepository.findByDoctorIdAndDate(doctorId, request.getDate());

        List<Slot> candidates = all.stream()
                .filter(s -> !s.getStartTime().isBefore(request.getStartingFrom()))
                .toList();

        // Windows occupied by non-candidates (real external conflicts)
        record Window(LocalTime start, LocalTime end) {}
        java.util.Set<Long> candidateIds = candidates.stream().map(Slot::getId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Window> occupiedByNonCandidates = all.stream()
                .filter(s -> !candidateIds.contains(s.getId()))
                .map(s -> new Window(s.getStartTime(), s.getEndTime()))
                .collect(java.util.stream.Collectors.toSet());

        int delta = request.effectiveShiftMinutes();

        int matched = candidates.size();
        int shifted = 0;
        int skipped = 0;

        java.util.List<Long> shiftedIds = new java.util.ArrayList<>();
        java.util.List<Long> skippedIds = new java.util.ArrayList<>();
        java.util.Map<Long,String> skippedReasons = new java.util.HashMap<>();
        java.util.Map<String,Integer> skipReasonCounts = new java.util.HashMap<>();

        // Track target windows inside this batch to avoid intra-batch duplicates
        java.util.Set<Window> plannedTargets = new java.util.HashSet<>();

        // First pass: PLAN (validate targets; don’t mutate yet)
        java.util.Map<Long, Window> plan = new java.util.HashMap<>();
        for (Slot slot : candidates) {
            try {
                LocalTime newStart = slot.getStartTime().plusMinutes(delta);
                LocalTime newEnd   = slot.getEndTime().plusMinutes(delta);


                if (!newEnd.isAfter(newStart)) {
                    addSkip(slot.getId(), "out_of_day_or_invalid", skippedReasons, skipReasonCounts, skippedIds);
                    skipped++;
                    continue;
                }

                Window target = new Window(newStart, newEnd);

                // Real conflict only if occupied by NON-candidate window
                if (occupiedByNonCandidates.contains(target)) {
                    addSkip(slot.getId(), "conflict_with_non_candidate", skippedReasons, skipReasonCounts, skippedIds);
                    skipped++; continue;
                }

                // Intra-batch duplicate target
                if (!plannedTargets.add(target)) {
                    addSkip(slot.getId(), "target_collision_within_batch", skippedReasons, skipReasonCounts, skippedIds);
                    skipped++; continue;
                }

                // All good — add to plan
                plan.put(slot.getId(), target);
            } catch (Exception ex) {
                addSkip(slot.getId(), "exception:" + ex.getClass().getSimpleName(), skippedReasons, skipReasonCounts, skippedIds);
                skipped++;
            }
        }

        // Second pass: APPLY (mutate in-memory)
        for (Slot slot : candidates) {
            Window target = plan.get(slot.getId());
            if (target != null) {
                slot.setStartTime(target.start());
                slot.setEndTime(target.end());

                //  Save reason in notes
                String reason = request.getReason() != null ? request.getReason() : "No reason provided";
                slot.setNotes("Shifted by " + delta + " mins. Reason: " + reason);
                
                shiftedIds.add(slot.getId());
                shifted++;
            }
        }

        // Persist unless dry-run
        if (!request.isDryRun()) {
            slotRepository.saveAll(candidates); // only changed ones will dirty-check
            if (request.isSendSms()) {
                notifySmsForShifted(candidates, shiftedIds, request);
            }
        }

        String dirWord = request.getDirection().name().toLowerCase();
        String msg = (request.isDryRun() ? "[DRY-RUN] " : "")
                + "Attempted to " + dirWord + " " + matched + " slot(s) by "
                + Math.abs(delta) + " minutes from " + request.getStartingFrom()
                + " on " + request.getDate() + ". Shifted=" + shifted + ", Skipped=" + skipped + ".";

        return ShiftAppointmentsResult.builder()
                .totalMatched(matched).totalShifted(shifted).totalSkipped(skipped)
                .shiftedIds(shiftedIds).skippedIds(skippedIds)
                .message(msg).skippedReasons(skippedReasons).skipReasonCounts(skipReasonCounts)
                .build();
    }

    private void notifySmsForShifted(List<Slot> candidates, java.util.List<Long> shiftedIds, SlotShiftRequestDto request) {
        for (Slot s : candidates) {
            if (shiftedIds.contains(s.getId()) && s.getAppointment() != null) {
                try {
                    smsService.notifyAppointmentRescheduled(
                            s.getAppointment().getId(),
                            s.getAppointment().getPatient().getId(),
                            s.getDate(),
                            s.getStartTime(),
                            request.getReason()
                    );
                } catch (Exception e) {
                    log.warn("SMS failed for appt {}: {}", s.getAppointment().getId(), e.getMessage());
                }
            }
        }
    }

    private static void addSkip(Long id, String reason,
                                java.util.Map<Long,String> perId,
                                java.util.Map<String,Integer> counts,
                                java.util.List<Long> ids) {
        ids.add(id);
        perId.put(id, reason);
        counts.merge(reason, 1, Integer::sum);
    }

    @Transactional
    public void blockSlots(Long doctorId, SlotBlockRequestDto request) {
        List<Slot> slots = slotRepository.findByDoctorIdAndDateAndStartTimeBetween(
                doctorId, request.getDate(), request.getStartTime(), request.getEndTime());

        for (Slot slot : slots) {
            if (request.isCancelExisting() && slot.getAppointment() != null) {
                Appointment appointment = slot.getAppointment();
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(appointment);
            }
            slot.setStatus(SlotStatus.BLOCKED);
        }
        slotRepository.saveAll(slots);
        log.info("Doctor {} blocked {} slots", doctorId, slots.size());
    }

    @Transactional
    public void unblockSlots(Long doctorId, SlotUnblockRequestDto request) {
        List<Slot> slots = slotRepository.findByDoctorIdAndDateAndStartTimeBetween(
                doctorId, request.getDate(), request.getStartTime(), request.getEndTime());

        for (Slot slot : slots) {
            if (slot.getStatus() == SlotStatus.BLOCKED) {
                slot.setStatus(SlotStatus.AVAILABLE);
            }
        }
        slotRepository.saveAll(slots);
        log.info("Doctor {} unblocked {} slots", doctorId, slots.size());
    }

    public List<SlotStatusDto> getSlotStatuses(Long doctorId, LocalDate date) {
        List<Slot> slots = slotRepository.findByDoctorIdAndDate(doctorId, date);
        return slots.stream().map(slot -> SlotStatusDto.builder()
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus().name())
                .type(slot.getType().name())
                .color(slot.getStatus().getColorCode())
                .build()).toList();
    }

    @Transactional
    public Appointment bookWalkInSlot(WalkInAppointmentDto dto) {
        List<Slot> slots = slotRepository.findByDoctorIdAndDateAndStatusIn(
                dto.getDoctorId(), dto.getDate(), List.of(SlotStatus.WALKIN, SlotStatus.AVAILABLE));

        Optional<Slot> available = slots.stream().filter(s -> s.getStartTime().equals(dto.getTime())).findFirst();

        if (available.isEmpty()) {
            throw new HospitalAPIException(HttpStatus.NOT_FOUND, "No walk-in slot available at given time");
        }

        Slot slot = available.get();
        slot.setStatus(SlotStatus.BOOKED);

        Appointment appointment = new Appointment();
        appointment.setDoctor(userRepository.findById(dto.getDoctorId()).orElseThrow());
        appointment.setPatient(userRepository.findById(dto.getPatientId()).orElseThrow());
        appointment.setSlot(slot);
        appointment.setDate(dto.getDate());
        appointment.setType(AppointmentType.WALKIN);
        appointment.setStatus(AppointmentStatus.BOOKED);

        slot.setAppointment(appointment);
        slotRepository.save(slot);
        return appointmentRepository.save(appointment);
    }

    public List<Slot> getEmergencySlots(Long doctorId, LocalDate date) {
        return slotRepository.findByDoctorIdAndDateAndStatusIn(
                doctorId, date, List.of(SlotStatus.WALKIN, SlotStatus.AVAILABLE));
    }

    @Override
    public Slot getSlotByDoctorAndTime(Long doctorId, LocalDate appointmentDate, LocalTime slotTime) {
        log.debug("🔍 Fetching slot for doctorId={}, date={}, time={}", doctorId, appointmentDate, slotTime);

        return slotRepository.findByDoctorIdAndStartTimeAndDate(doctorId, slotTime, appointmentDate)
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.NOT_FOUND, "No slot found at this time for the doctor"));

    }


}

