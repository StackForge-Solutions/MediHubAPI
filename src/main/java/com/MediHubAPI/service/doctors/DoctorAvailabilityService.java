package com.MediHubAPI.service.doctors;

import com.MediHubAPI.dto.doctor.DoctorSessionDto;
import com.MediHubAPI.dto.doctor.DoctorSessionsData;
import com.MediHubAPI.dto.doctor.DoctorSlotDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Slot;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.model.enums.SlotType;
import com.MediHubAPI.repository.SlotRepository;
import com.MediHubAPI.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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
    private static final List<SlotStatus>  PUBLISHED_SLOT_STATUSES = List.of(
            SlotStatus.AVAILABLE,
            SlotStatus.ADDITIONAL,
            SlotStatus.WALKIN,
            SlotStatus.BOOKED,
            SlotStatus.ARRIVED,
            SlotStatus.COMPLETED,
            SlotStatus.RESERVED,
            SlotStatus.NO_SHOW,
            SlotStatus.PENDING
    );

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    @Transactional
    public
    DoctorSessionsData getSessions(Long doctorId) {
        findDoctor(doctorId); // ensure exists+role
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd   = weekStart.plusDays(6);
        List<Slot> slots    = slotRepository.findByDoctorIdAndDateBetweenAndStatusInOrderByDateAscStartTimeAsc(
                doctorId, weekStart, weekEnd, PUBLISHED_SLOT_STATUSES
        );
        if (slots.isEmpty()) {
            throw new HospitalAPIException(
                    HttpStatus.NOT_FOUND, "NOT_FOUND",
                    "No published schedule found for doctor " + doctorId
            );
        }

        Map<String, List<DoctorSessionDto>> sessionsByWeekday = initializeWeekMap();
        fillSessionsFromSlots(slots, sessionsByWeekday);

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

        List<Slot> slots = slotRepository.findByDoctorIdAndDate(doctorId, date);
        if (slots.isEmpty()) {
            throw new HospitalAPIException(
                    HttpStatus.NOT_FOUND, "NOT_FOUND",
                    "No slots found for doctor " + doctorId + " on " + date
            );
        }

        return slots.stream()
                    .filter(slot -> durationMin == null || durationMatches(slot, durationMin))
                    .sorted(Comparator.comparing(Slot::getStartTime))
                    .map(slot -> mapSlot(slot, doctorId, date))
                    .collect(Collectors.toList());
    }

    private
    boolean durationMatches(Slot slot, Integer durationMin) {
        var start   = slot.getStartTime();
        var end     = slot.getEndTime();
        int minutes = (int) java.time.Duration.between(start, end).toMinutes();
        return minutes == durationMin;
    }

    private
    DoctorSlotDto mapSlot(Slot slot, Long doctorId, LocalDate date) {
        return DoctorSlotDto.builder()
                            .doctorId(doctorId)
                            .dateISO(date.toString())
                            .timeHHmm(slot.getStartTime().format(TIME_FORMAT))
                            .status(slot.getStatus().name())
                            .isWalkin(slot.getType() == SlotType.WALKIN || slot.getStatus() == SlotStatus.WALKIN)
                            .build();
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
    void fillSessionsFromSlots(List<Slot> weekSlots, Map<String, List<DoctorSessionDto>> sessionsByWeekday) {
        EnumMap<DayOfWeek, List<Slot>> slotsByDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dow : DayOfWeek.values()) {
            slotsByDay.put(dow, new ArrayList<>());
        }
        for (Slot slot : weekSlots) {
            slotsByDay.get(slot.getDate().getDayOfWeek()).add(slot);
        }
        for (DayOfWeek dow : DayOfWeek.values()) {
            List<Slot> daySlots = slotsByDay.get(dow);
            if (daySlots.isEmpty()) {
                continue;
            }
            daySlots.sort(Comparator.comparing(Slot::getStartTime));
            int idx = 1;
            List<DoctorSessionDto> target = sessionsByWeekday.get(String.valueOf(dow.getValue()));
            for (Slot slot : daySlots) {
                target.add(buildSessionDto(idx++, slot.getStartTime(), slot.getEndTime()));
            }
        }
    }

    private
    DoctorSessionDto buildSessionDto(int index, LocalTime start, LocalTime end) {
        return DoctorSessionDto.builder()
                               .label("Session " + index)
                               .startHHmm(start.format(TIME_FORMAT))
                               .endHHmm(end.format(TIME_FORMAT))
                               .build();
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
