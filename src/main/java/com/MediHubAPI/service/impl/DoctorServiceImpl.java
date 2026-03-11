package com.MediHubAPI.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;
import com.MediHubAPI.dto.DoctorAvailabilityDto;
import com.MediHubAPI.dto.DoctorProfileDto;
import com.MediHubAPI.dto.DoctorSearchCriteria;
import com.MediHubAPI.dto.SlotResponseDto;
import com.MediHubAPI.dto.SpecializationOptionDto;
import com.MediHubAPI.dto.UserDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.exception.ResourceNotFoundException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Slot;
import com.MediHubAPI.model.Specialization;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.model.enums.SlotType;
import com.MediHubAPI.repository.SlotRepository;
import com.MediHubAPI.repository.SpecializationRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.DoctorService;
import com.MediHubAPI.specification.DoctorSpecification;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final UserRepository userRepository;
    private final SlotRepository slotRepository;
    private final SpecializationRepository specializationRepository;
    private final ModelMapper modelMapper;

    @Override
    public Page<UserDto> searchDoctors(DoctorSearchCriteria criteria, Pageable pageable) {
        Specification<User> spec = new DoctorSpecification(criteria);
        return userRepository.findAll(spec, pageable)
                .map(user -> modelMapper.map(user, UserDto.class));
    }

    @Override
    public List<SpecializationOptionDto> getSpecializations() {
        return specializationRepository.findAll().stream()
                .sorted(Comparator.comparing(Specialization::getName, String.CASE_INSENSITIVE_ORDER))
                .map(specialization -> new SpecializationOptionDto(specialization.getId(), specialization.getName()))
                .toList();
    }

    @Override
    public DoctorProfileDto getDoctorById(Long id) {
        User doctor = validateDoctor(id);
        return modelMapper.map(doctor, DoctorProfileDto.class);
    }

    /**
     * Defines the availability slots for a given doctor.
     *
     * <p>This method supports two types of availability:</p>
     * <ul>
     *     <li><b>Weekly recurring availability</b>: Optional. Repeats weekly for the next 4 weeks.</li>
     *     <li><b>Date-specific availability</b>: Optional. One-time availability for exact dates.</li>
     * </ul>
     *
     * @param doctorId the ID of the doctor
     * @param dto the availability DTO containing slot duration and one or both types of availability
     * @throws HospitalAPIException if the doctor does not exist or an error occurs during slot generation
     */
    @Override
    public void defineAvailability(Long doctorId, DoctorAvailabilityDto dto) {
        // Validate doctor existence
        User doctor = validateDoctor(doctorId);

        // Validate slot duration
        Integer duration = dto.getSlotDurationInMinutes();
        if (duration == null || duration <= 0 || duration > 240) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Slot duration must be between 1 and 240 minutes.");
        }

        boolean hasWeekly = dto.getWeeklyAvailability() != null && !dto.getWeeklyAvailability().isEmpty();
        boolean hasDateWise = dto.getDateWiseAvailability() != null && !dto.getDateWiseAvailability().isEmpty();

        if (!hasWeekly && !hasDateWise) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST,
                    "Please provide either weeklyAvailability or dateWiseAvailability.");
        }

        // Track already-added time ranges per date to prevent overlaps
        Map<LocalDate, List<DoctorAvailabilityDto.TimeRange>> slotTracker = new HashMap<>();

        // === Weekly Availability ===
        if (hasWeekly) {
            dto.getWeeklyAvailability().forEach((dayOfWeek, timeRanges) -> {
                for (DoctorAvailabilityDto.TimeRange range : timeRanges) {
                    validateTimeRange(range, "weeklyAvailability");

                    for (int week = 0; week < 4; week++) {
                        LocalDate date = LocalDate.now()
                                .with(java.time.temporal.TemporalAdjusters.nextOrSame(dayOfWeek))
                                .plusWeeks(week);

                        checkOverlap(slotTracker, date, range);
                        generateSlotsWithConflictCheck(doctor, date, range.getStart(), range.getEnd(), duration);
                    }
                }
            });
        }

        // === Date-wise Availability ===
        if (hasDateWise) {
            Set<LocalDate> seenDates = new HashSet<>();
            dto.getDateWiseAvailability().forEach((date, timeRanges) -> {
                if (!seenDates.add(date)) {
                    throw new HospitalAPIException(HttpStatus.CONFLICT, "Duplicate date entry: " + date);
                }

                for (DoctorAvailabilityDto.TimeRange range : timeRanges) {
                    validateTimeRange(range, "dateWiseAvailability");
                    checkOverlap(slotTracker, date, range);
                    generateSlotsWithConflictCheck(doctor, date, range.getStart(), range.getEnd(), duration);
                }
            });
        }
    }

    private void validateTimeRange(DoctorAvailabilityDto.TimeRange range, String source) {
        if (range.getStart() == null || range.getEnd() == null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Start and end time must be provided in " + source);
        }
        if (!range.getStart().isBefore(range.getEnd())) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "End time must be after start time in " + source);
        }
    }

    private void checkOverlap(Map<LocalDate, List<DoctorAvailabilityDto.TimeRange>> tracker,
            LocalDate date,
            DoctorAvailabilityDto.TimeRange newRange) {
        List<DoctorAvailabilityDto.TimeRange> existing = tracker.computeIfAbsent(date, d -> new ArrayList<>());
        for (DoctorAvailabilityDto.TimeRange r : existing) {
            if (isOverlapping(r.getStart(), r.getEnd(), newRange.getStart(), newRange.getEnd())) {
                throw new HospitalAPIException(
                        HttpStatus.CONFLICT,
                        String.format("Overlapping time blocks on %s: %s–%s overlaps with %s–%s", date, r.getStart(),
                                r.getEnd(), newRange.getStart(), newRange.getEnd())
                );
            }
        }
        existing.add(newRange);
    }

    private boolean isOverlapping(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !start1.isAfter(end2.minusSeconds(1)) && !start2.isAfter(end1.minusSeconds(1));
    }

    private void generateSlotsWithConflictCheck(User doctor,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            int duration) {
        LocalTime current = start;

        while (!current.plusMinutes(duration).isAfter(end)) {
            LocalTime slotEnd = current.plusMinutes(duration);

            // Check if already booked
            boolean exists = slotRepository.existsByDoctorIdAndDateAndStartTimeAndEndTime(
                    doctor.getId(), date, current, slotEnd);
            if (exists) {
                throw new HospitalAPIException(
                        HttpStatus.CONFLICT,
                        String.format("Slot at %s already booked for %s", current, date)
                );

            }

            // Save slot logic here (if needed)
            // slotRepository.save(new Slot(doctor, date, current, slotEnd));
            Slot slot = Slot.builder()
                    .doctor(doctor)
                    .date(date)
                    .startTime(current)
                    .endTime(slotEnd)
                    .status(SlotStatus.AVAILABLE)
                    .type(SlotType.REGULAR) //  REQUIRED to avoid null
                    .recurring(false)       // or true if this is from weekly template
                    .build();

            slotRepository.save(slot);


            current = slotEnd;
        }
    }


    public void defineAvailabilityByDay(Long doctorId, DoctorAvailabilityDto dto) {
        User doctor = validateDoctor(doctorId);

        int duration = dto.getSlotDurationInMinutes();
        Map<DayOfWeek, List<DoctorAvailabilityDto.TimeRange>> availabilityMap = dto.getWeeklyAvailability();

        availabilityMap.forEach((day, timeRanges) -> {
            for (DoctorAvailabilityDto.TimeRange range : timeRanges) {
                for (int week = 0; week < 4; week++) {
                    LocalDate targetDate = LocalDate.now()
                            .with(java.time.temporal.TemporalAdjusters.nextOrSame(day))
                            .plusWeeks(week);

                    generateWeeklySlots(doctor, targetDate, range.getStart(), range.getEnd(), duration);
                }
            }
        });
    }

    @Override
    public void updateAvailability(Long id, DoctorAvailabilityDto dto) {
        defineAvailability(id, dto); // reuse template method
    }


    @Override
    public List<SlotResponseDto> getSlotsForDate(Long doctorId, LocalDate date) {
        validateDoctor(doctorId);
        List<Slot> slots = slotRepository.findByDoctorIdAndDate(doctorId, date);
        log.info(" Fetched {} slots for doctorId={} on {}", slots.size(), doctorId, date);
        return slots.stream()
                .map(slot -> modelMapper.map(slot, SlotResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public void deactivateDoctor(Long id) {
        User user = validateDoctor(id);
        user.setEnabled(false);
        userRepository.save(user);
        log.warn("⚠️ Doctor with ID={} has been deactivated", id);
    }

    @Override
    public void deleteDoctor(Long id) {
        User user = validateDoctor(id);
        userRepository.delete(user);
        log.warn("🗑️ Doctor with ID={} has been deleted", id);
    }

    // 🔁 Template Method to generate and replace slots for a day
    private void generateWeeklySlots(User doctor, LocalDate date, LocalTime start, LocalTime end, int duration) {
        log.info("📅 Generating slots for Doctor={} Date={} Time={}–{}", doctor.getId(), date, start, end);

        // Delete existing slots
        List<Slot> existing = slotRepository.findByDoctorIdAndDate(doctor.getId(), date);
        if (!existing.isEmpty()) {
            slotRepository.deleteAll(existing);
            log.warn("🧹 Deleted {} old slots for Doctor={} Date={}", existing.size(), doctor.getId(), date);
        }

        // Create new slots
        List<Slot> slots = new ArrayList<>();
        LocalTime current = start;
        while (!current.plusMinutes(duration).isAfter(end)) {
            slots.add(Slot.builder()
                    .doctor(doctor)
                    .date(date)
                    .startTime(current)
                    .endTime(current.plusMinutes(duration))
                    .status(SlotStatus.AVAILABLE)
                    .type(SlotType.REGULAR)
                    .build());
            current = current.plusMinutes(duration);
        }

        slotRepository.saveAll(slots);
        log.info(" Created {} new slots for Doctor={} Date={}", slots.size(), doctor.getId(), date);
    }

    private User validateDoctor(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id=", "id", id));

        boolean isDoctor = user.getRoles().stream().anyMatch(r -> r.getName() == ERole.DOCTOR);
        if (!isDoctor) {
            log.error("❌ User ID={} is not a doctor", id);
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "User is not a doctor");
        }
        return user;
    }
}
