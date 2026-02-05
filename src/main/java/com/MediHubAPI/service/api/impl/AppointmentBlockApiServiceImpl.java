package com.MediHubAPI.service.api.impl;

import com.MediHubAPI.dto.appointments.AppointmentBlockRequest;
import com.MediHubAPI.dto.appointments.AppointmentBlockResponse;
import com.MediHubAPI.dto.appointments.AppointmentShiftRequest;
import com.MediHubAPI.dto.appointments.AppointmentShiftResponse;
import com.MediHubAPI.dto.appointments.AppointmentUnblockRequest;
import com.MediHubAPI.dto.appointments.AppointmentUnblockResponse;
import com.MediHubAPI.dto.SlotShiftRequestDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.Slot;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.AppointmentStatus;
import com.MediHubAPI.model.enums.AppointmentOperationType;
import com.MediHubAPI.model.enums.InitiatorRole;
import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.SlotRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.AppointmentOperationService;
import com.MediHubAPI.service.DoctorDateLockProvider;
import com.MediHubAPI.service.SlotService;
import com.MediHubAPI.service.api.AppointmentBlockApiService;
import com.MediHubAPI.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public
class AppointmentBlockApiServiceImpl implements AppointmentBlockApiService {

    private static final DateTimeFormatter               TIME_FORMATTER       = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<com.MediHubAPI.model.ERole> STAFF_ROLE_WHITELIST = Set.of(
            com.MediHubAPI.model.ERole.SUPER_ADMIN,
            com.MediHubAPI.model.ERole.ADMIN,
            com.MediHubAPI.model.ERole.HR_MANAGER,
            com.MediHubAPI.model.ERole.PHARMACIST,
            com.MediHubAPI.model.ERole.NURSE,
            com.MediHubAPI.model.ERole.RECEPTIONIST,
            com.MediHubAPI.model.ERole.BILLING_CLERK
                                                                                      );

    private final SlotRepository              slotRepository;
    private final AppointmentRepository       appointmentRepository;
    private final SlotService                 slotService;
    private final DoctorDateLockProvider      lockProvider;
    private final AppointmentOperationService operationService;
    private final UserRepository              userRepository;

    @Override
    public
    AppointmentBlockResponse blockSlots(String idempotencyKey, AppointmentBlockRequest request) {
        validateIdempotencyKey(idempotencyKey);
        LocalDate date  = parseDate(request.getDateISO());
        LocalTime start = parseTime(request.getStartHHmm(), "startHHmm must be a valid HH:mm");
        LocalTime end   = parseTime(request.getEndHHmm(), "endHHmm must be a valid HH:mm");
        if (!end.isAfter(start)) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "endHHmm must be after startHHmm.");
        }

        String fingerprint = HashUtil.sha256(buildBlockFingerprint(request, date, start, end));
        AppointmentOperationService.AppointmentOperationMeta<AppointmentBlockResponse> meta =
                new AppointmentOperationService.AppointmentOperationMeta<>(
                        AppointmentOperationType.BLOCK,
                        request.getDoctorId(),
                        date,
                        fingerprint,
                        AppointmentBlockResponse.class
                );

        return lockProvider.executeWithLock(
                request.getDoctorId(), date, () ->
                        operationService.execute(
                                idempotencyKey, meta, () ->
                                        performBlock(request, date, start, end)
                                                )
                                           );
    }

    @Override
    public
    AppointmentUnblockResponse unblockSlots(String idempotencyKey, AppointmentUnblockRequest request) {
        validateIdempotencyKey(idempotencyKey);
        LocalDate date        = parseDate(request.getDateISO());
        String    fingerprint = HashUtil.sha256(buildUnblockFingerprint(request, date));
        AppointmentOperationService.AppointmentOperationMeta<AppointmentUnblockResponse> meta =
                new AppointmentOperationService.AppointmentOperationMeta<>(
                        AppointmentOperationType.UNBLOCK,
                        request.getDoctorId(),
                        date,
                        fingerprint,
                        AppointmentUnblockResponse.class
                );
        return lockProvider.executeWithLock(
                request.getDoctorId(), date, () ->
                        operationService.execute(
                                idempotencyKey, meta, () ->
                                        performUnblock(request, date)
                                                )
                                           );
    }

    @Override
    public
    AppointmentShiftResponse shiftAppointments(String idempotencyKey, AppointmentShiftRequest request) {
        validateIdempotencyKey(idempotencyKey);
        LocalDate date      = parseDate(request.getDateISO());
        LocalTime startFrom = parseTime(request.getStartFromHHmm(), "startFromHHmm must be a valid HH:mm");
        if (request.getMinutes() <= 0) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "minutes must be greater than 0.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new HospitalAPIException(
                    HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authenticated user is required "
                                                              + "for shifting appointments."
            );
        }
        User user = userRepository.findByUsernameOrEmailWithRoles(auth.getName())
                                  .orElseThrow(() -> new HospitalAPIException(
                                          HttpStatus.UNAUTHORIZED,
                                          "USER_NOT_FOUND", "Authenticated user "
                                                            + "not found."
                                  ));
        InitiatorRole initiatorRole = resolveInitiatorRole(user);

        String fingerprint = HashUtil.sha256(buildShiftFingerprint(request, date, startFrom));
        AppointmentOperationService.AppointmentOperationMeta<AppointmentShiftResponse> meta =
                new AppointmentOperationService.AppointmentOperationMeta<>(
                        AppointmentOperationType.SHIFT,
                        request.getDoctorId(),
                        date,
                        fingerprint,
                        AppointmentShiftResponse.class
                );

        return lockProvider.executeWithLock(
                request.getDoctorId(), date, () ->
                        operationService.execute(
                                idempotencyKey, meta, () ->
                                        performShift(request, date, startFrom, user.getId(), initiatorRole)
                                                )
                                           );
    }

    private
    AppointmentBlockResponse performBlock(
            AppointmentBlockRequest request,
            LocalDate date,
            LocalTime start,
            LocalTime end
                                         ) {
        List<Slot> candidates = slotRepository.findByDoctorIdAndDateAndStartTimeBetween(
                request.getDoctorId(), date, start, end);

        List<Slot>   updatedSlots = new ArrayList<>();
        List<String> blockedKeys  = new ArrayList<>();
        int          cancelled    = 0;
        for (Slot slot : candidates) {
            LocalTime slotStart = slot.getStartTime();
            if (slotStart.isBefore(start) || !slotStart.isBefore(end)) {
                continue;
            }
            Appointment appointment = slot.getAppointment();
            if (request.isCancelBooked() && appointment != null && appointment.getStatus() != AppointmentStatus.CANCELLED) {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(appointment);
                cancelled++;
            }
            slot.setStatus(SlotStatus.BLOCKED);
            updatedSlots.add(slot);
            blockedKeys.add(formatSlotKey(request.getDoctorId(), date, slotStart));
        }
        if (!updatedSlots.isEmpty()) {
            slotRepository.saveAll(updatedSlots);
        }
        blockedKeys.sort(String::compareTo);
        return AppointmentBlockResponse.builder()
                                       .blockedSlots(blockedKeys)
                                       .cancelledAppointments(cancelled)
                                       .build();
    }

    private
    AppointmentUnblockResponse performUnblock(
            AppointmentUnblockRequest request,
            LocalDate date
                                             ) {
        List<Slot>   slots     = slotRepository.findByDoctorIdAndDate(request.getDoctorId(), date);
        List<Slot>   toUpdate  = new ArrayList<>();
        List<String> unblocked = new ArrayList<>();
        for (Slot slot : slots) {
            if (slot.getStatus() == SlotStatus.BLOCKED) {
                slot.setStatus(SlotStatus.AVAILABLE);
                toUpdate.add(slot);
                unblocked.add(formatSlotKey(request.getDoctorId(), date, slot.getStartTime()));
            }
        }
        if (!toUpdate.isEmpty()) {
            slotRepository.saveAll(toUpdate);
        }
        unblocked.sort(String::compareTo);
        return AppointmentUnblockResponse.builder()
                                         .unblockedSlots(unblocked)
                                         .build();
    }

    private
    AppointmentShiftResponse performShift(
            AppointmentShiftRequest request,
            LocalDate date,
            LocalTime startFrom,
            Long initiatorId,
            InitiatorRole initiatorRole
                                         ) {
        SlotShiftRequestDto shiftDto = SlotShiftRequestDto.builder()
                                                          .date(date)
                                                          .shiftByDays(0)
                                                          .shiftByHours(0)
                                                          .shiftByMinutes(request.getMinutes())
                                                          .startingFrom(startFrom)
                                                          .sendSms(request.isSendSms())
                                                          .reason(StringUtils.hasText(request.getReason()) ?
                                                                  request.getReason().trim() : "")
                                                          .direction(request.getDirection())
                                                          .initiatedByUserId(initiatorId)
                                                          .initiatedByRole(initiatorRole)
                                                          .onlyBooked(true)
                                                          .includeBlocked(false)
                                                          .dryRun(false)
                                                          .build()
                ;

        var result = slotService.shiftSlots(request.getDoctorId(), shiftDto);
        return AppointmentShiftResponse.builder()
                                       .shiftedCount(result.getTotalShifted())
                                       .attentionCount(result.getTotalSkipped())
                                       .build();
    }

    private static
    String formatSlotKey(Long doctorId, LocalDate date, LocalTime time) {
        return doctorId + "|" + date + "|" + time.format(TIME_FORMATTER);
    }

    private
    void validateIdempotencyKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new HospitalAPIException(
                    HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Idempotency-Key header is "
                                                             + "required."
            );
        }
    }

    private static
    String buildBlockFingerprint(
            AppointmentBlockRequest request,
            LocalDate date,
            LocalTime start,
            LocalTime end
                                ) {
        return request.getDoctorId() + "|" + date + "|" + start + "|" + end + "|" + request.isCancelBooked();
    }

    private static
    String buildUnblockFingerprint(AppointmentUnblockRequest request, LocalDate date) {
        return request.getDoctorId() + "|" + date;
    }

    private static
    String buildShiftFingerprint(
            AppointmentShiftRequest request,
            LocalDate date,
            LocalTime startFrom
                                ) {
        String reason = request.getReason() == null ? "" : request.getReason().trim();
        return request.getDoctorId() + "|" + date + "|" + startFrom + "|" + request.getMinutes() + "|"
               + request.getDirection() + "|" + request.isSendSms() + "|" + reason;
    }

    private static
    LocalDate parseDate(String iso) {
        try {
            return LocalDate.parse(iso);
        }
        catch (DateTimeParseException ex) {
            throw new HospitalAPIException(
                    HttpStatus.BAD_REQUEST, "INVALID_INPUT", "dateISO must be a valid ISO date"
                                                             + "."
            );
        }
    }

    private static
    LocalTime parseTime(String hhmm, String errorMsg) {
        try {
            return LocalTime.parse(hhmm, TIME_FORMATTER);
        }
        catch (DateTimeParseException ex) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", errorMsg);
        }
    }

    private static
    InitiatorRole resolveInitiatorRole(User user) {
        boolean isDoctor = user.getRoles().stream()
                               .map(com.MediHubAPI.model.Role::getName)
                               .anyMatch(role -> role == com.MediHubAPI.model.ERole.DOCTOR)
                ;
        if (isDoctor) {
            return InitiatorRole.DOCTOR;
        }
        boolean isStaff = user.getRoles().stream()
                              .map(com.MediHubAPI.model.Role::getName)
                              .anyMatch(STAFF_ROLE_WHITELIST::contains)
                ;
        if (isStaff) {
            return InitiatorRole.STAFF;
        }
        throw new HospitalAPIException(
                HttpStatus.FORBIDDEN, "INVALID_INITIATOR", "Only doctors or staff members can "
                                                           + "perform this operation."
        );
    }
}
