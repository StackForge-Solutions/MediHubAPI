package com.MediHubAPI.service.doctors;

import com.MediHubAPI.dto.doctor.DoctorSessionDto;
import com.MediHubAPI.dto.doctor.DoctorSessionsData;
import com.MediHubAPI.dto.doctor.DoctorSlotDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.ScheduleStatus;
import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.model.enums.SlotType;
import com.MediHubAPI.model.scheduling.session.SessionSchedule;
import com.MediHubAPI.model.scheduling.session.SessionScheduleDay;
import com.MediHubAPI.model.scheduling.session.SessionScheduleInterval;
import com.MediHubAPI.repository.SlotRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.repository.scheduling.session.SessionScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public
class DoctorAvailabilityService {

    private static final DateTimeFormatter TIME_FORMAT            = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<Integer>     ALLOWED_SLOT_DURATIONS = List.of(10, 15, 30);

    private final SlotRepository            slotRepository;
    private final UserRepository            userRepository;
    private final SessionScheduleRepository sessionScheduleRepository;

    @Transactional
    public
    DoctorSessionsData getSessions(Long doctorId) {
        findDoctor(doctorId); // ensure exists+role
        LocalDate       weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        SessionSchedule schedule  = findEffectiveSchedule(doctorId, weekStart);
        if (schedule == null) {
            throw new HospitalAPIException(
                    HttpStatus.NOT_FOUND, "NOT_FOUND",
                    "No schedule found for doctor " + doctorId
            );
        }

        Map<String, List<DoctorSessionDto>> sessionsByWeekday = initializeWeekMap();
        if (!CollectionUtils.isEmpty(schedule.getDays())) {
            schedule.getDays().stream()
                    .sorted(Comparator.comparingInt(d -> d.getDayOfWeek().getValue()))
                    .forEach(day -> populateDaySessions(day, sessionsByWeekday))
            ;
        }

        return DoctorSessionsData.builder()
                                 .doctorId(doctorId)
                                 .sessionsByWeekday(sessionsByWeekday)
                                 .build();
    }

    @Transactional
    public
    List<DoctorSlotDto> getSlots(Long doctorId, LocalDate date, Integer durationMin) {
        findDoctor(doctorId);

        if (durationMin != null && !ALLOWED_SLOT_DURATIONS.contains(durationMin)) {
            throw new HospitalAPIException(
                    HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                    "durationMin must be one of " + ALLOWED_SLOT_DURATIONS
            );
        }

        List<com.MediHubAPI.model.Slot> slots = slotRepository.findByDoctorIdAndDate(doctorId, date);
        if (slots.isEmpty()) {
            throw new HospitalAPIException(
                    HttpStatus.NOT_FOUND, "NOT_FOUND",
                    "No slots found for doctor " + doctorId + " on " + date
            );
        }

        return slots.stream()
                    .filter(slot -> durationMin == null || durationMatches(slot, durationMin))
                    .sorted(Comparator.comparing(com.MediHubAPI.model.Slot::getStartTime))
                    .map(slot -> mapSlot(slot, doctorId, date))
                    .collect(Collectors.toList());
    }

    private
    boolean durationMatches(com.MediHubAPI.model.Slot slot, Integer durationMin) {
        var start   = slot.getStartTime();
        var end     = slot.getEndTime();
        int minutes = (int) java.time.Duration.between(start, end).toMinutes();
        return minutes == durationMin;
    }

    private
    DoctorSlotDto mapSlot(com.MediHubAPI.model.Slot slot, Long doctorId, LocalDate date) {
        return DoctorSlotDto.builder()
                            .doctorId(doctorId)
                            .dateISO(date.toString())
                            .timeHHmm(slot.getStartTime().format(TIME_FORMAT))
                            .status(slot.getStatus().name())
                            .isWalkin(slot.getType() == SlotType.WALKIN || slot.getStatus() == SlotStatus.WALKIN)
                            .build();
    }

    private
    void populateDaySessions(SessionScheduleDay day, Map<String, List<DoctorSessionDto>> map) {
        List<DoctorSessionDto> target = map.get(String.valueOf(day.getDayOfWeek().getValue()));
        if (target == null) return;
        if (day.getIntervals() == null || day.getIntervals().isEmpty()) {
            return;
        }
        List<SessionScheduleInterval> sorted = day.getIntervals().stream()
                                                  .sorted(Comparator.comparing(SessionScheduleInterval::getStartTime))
                                                  .toList()
                ;
        int idx = 1;
        for (SessionScheduleInterval interval : sorted) {
            target.add(new DoctorSessionDto(
                    "Session " + idx++,
                    interval.getStartTime().format(TIME_FORMAT),
                    interval.getEndTime().format(TIME_FORMAT)
            ));
        }
    }

    private
    Map<String, List<DoctorSessionDto>> initializeWeekMap() {
        Map<String, List<DoctorSessionDto>> map = new LinkedHashMap<>();
        for (int dow = 1; dow <= 7; dow++) {
            map.put(String.valueOf(dow), new ArrayList<>());
        }
        return map;
    }

    private
    SessionSchedule findEffectiveSchedule(Long doctorId, LocalDate weekStart) {
        SessionSchedule override = chooseBest(sessionScheduleRepository
                                                      .findWithChildrenByModeAndDoctorIdAndWeekStartDate(ScheduleMode.DOCTOR_OVERRIDE, doctorId, weekStart));
        if (override != null) {
            return override;
        }
        return chooseBest(sessionScheduleRepository
                                  .findWithChildrenByModeAndWeekStartDate(ScheduleMode.GLOBAL_TEMPLATE, weekStart));
    }

    private
    SessionSchedule chooseBest(List<SessionSchedule> schedules) {
        return schedules.stream()
                        .filter(s -> s.getStatus() != ScheduleStatus.ARCHIVED)
                        .max(Comparator.comparingInt(this::statusPriority)
                                       .thenComparing(
                                               SessionSchedule::getVersion,
                                               Comparator.nullsFirst(Long::compareTo)
                                                     )
                                       .thenComparing(SessionSchedule::getId, Comparator.nullsFirst(Long::compareTo)))
                        .orElse(null);
    }

    private
    int statusPriority(SessionSchedule schedule) {
        return switch (schedule.getStatus()) {
            case PUBLISHED -> 3;
            case DRAFT -> 2;
            default -> 1;
        };
    }

    private
    User findDoctor(Long doctorId) {
        return userRepository.findById(doctorId)
                             .filter(user -> user.getRoles().stream().anyMatch(role -> role.getName() == ERole.DOCTOR))
                             .orElseThrow(() -> new HospitalAPIException(
                                     HttpStatus.NOT_FOUND, "NOT_FOUND", "Doctor "
                                                                        + "not "
                                                                        + "found:"
                                                                        + " " + doctorId
                             ));
    }
}
