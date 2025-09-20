package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.Slot;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.AppointmentStatus;
import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.AppointmentService;
import com.MediHubAPI.service.SlotService;
import com.MediHubAPI.specification.DoctorSpecification;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final ModelMapper modelMapper;

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final SlotService slotService;  // ✅ Injected


    @Override
    public AppointmentResponseDto bookAppointment(AppointmentBookingDto dto) {
        return bookAppointment(dto, null); // ✅ simply forward with null
    }

    @Override
    public AppointmentResponseDto bookAppointment(AppointmentBookingDto dto, Appointment rescheduledFrom) {
        log.debug("🔁 Booking appointment: {}", dto);

        // Step 1: Validate doctor and patient
        User doctor = findUserOrThrow(dto.getDoctorId(), "Doctor");
        User patient = findUserOrThrow(dto.getPatientId(), "Patient");

        // Step 2: Check if the slot exists and is marked AVAILABLE
        Slot slot = slotService.getSlotByDoctorAndTime(doctor.getId(), dto.getAppointmentDate(), dto.getSlotTime());
        if (slot == null || slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new HospitalAPIException(HttpStatus.CONFLICT, "Slot is not available");
        }

        // Step 3: Double-check slot conflict in appointments table (historical or manual cases)
        // This guards against cases where slot data and appointment table may diverge or race conditions exist
        validateSlotConflicts(doctor, patient, dto.getAppointmentDate(), dto.getSlotTime());

        // Step 4: Book appointment
        Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .appointmentDate(dto.getAppointmentDate())
                .slotTime(dto.getSlotTime())
                .type(dto.getAppointmentType())
                .status(AppointmentStatus.BOOKED)
                .rescheduledFrom(rescheduledFrom) // ✅ Set original appointment
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("✅ Appointment booked successfully: {}", saved.getId());



        AppointmentResponseDto response = modelMapper.map(saved, AppointmentResponseDto.class);
        response.setSlot(new SlotInfoDto(
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        ));

        return response;

    }

    @Override
    public List<AppointmentResponseDto> getAppointmentsForDoctor(Long doctorId, LocalDate date) {
        log.debug("📄 Fetching appointments for doctorId={} on {}", doctorId, date);
        return appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date).stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponseDto> getAppointmentsForPatient(Long patientId) {
        log.debug("📄 Fetching appointments for patientId={}", patientId);
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId).stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public Page<AppointmentResponseDto> getAppointmentsForPatient(Long patientId, Pageable pageable) {
        log.debug("📄 Fetching paginated appointments for patientId={} with pageable={}", patientId, pageable);
        return appointmentRepository.findByPatientId(patientId, pageable)
                .map(appointment -> modelMapper.map(appointment, AppointmentResponseDto.class));
    }

    @Override
    public Page<AppointmentResponseDto> getAppointmentsWithFilters(LocalDate date, String doctorName, AppointmentStatus status, String range, Pageable pageable) {
        log.debug("📄 Filtering appointments with date={}, range={}, doctorName={}, status={}", date, range, doctorName, status);

        Specification<Appointment> spec = buildAppointmentFilterSpec(date, range, doctorName, status);

        return appointmentRepository.findAll(spec, pageable)
                .map(appointment -> modelMapper.map(appointment, AppointmentResponseDto.class));
    }
    private Specification<Appointment> buildAppointmentFilterSpec(LocalDate date, String range, String doctorName, AppointmentStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            LocalDate today = LocalDate.now();
            LocalDate startDate =  null;
            LocalDate endDate = null;

            // Determine date filtering logic
            if (date != null) {
                predicates.add(cb.equal(root.get("appointmentDate"), date));
            } else if (range != null) {
                switch (range.toUpperCase()) {
                    case "TODAY" -> predicates.add(cb.equal(root.get("appointmentDate"), today));
                    case "PREV" -> predicates.add(cb.equal(root.get("appointmentDate"), today.minusDays(1)));
                    case "NEXT", "DAY" -> predicates.add(cb.equal(root.get("appointmentDate"), today.plusDays(1)));
                    case "WEEK" -> {
                        startDate = today.minusDays(6);
                        endDate = today;
                        predicates.add(cb.between(root.get("appointmentDate"), startDate, endDate));
                    }
                    case "MONTH" -> {
                        startDate = today.withDayOfMonth(1);
                        endDate = today;
                        predicates.add(cb.between(root.get("appointmentDate"), startDate, endDate));
                    }
                    default -> log.warn("⚠️ Unknown range: '{}'", range);
                }
            }

            // Doctor name filtering
            // Doctor full name filtering
            if (doctorName != null && !doctorName.isBlank()) {
                Path<String> firstName = root.get("doctor").get("firstName");
                Path<String> lastName = root.get("doctor").get("lastName");

                // CONCAT(lower(firstName), ' ', lower(lastName))
                Expression<String> fullName = cb.concat(cb.lower(firstName), cb.literal(" "));
                fullName = cb.concat(fullName, cb.lower(lastName));

                predicates.add(cb.like(fullName, "%" + doctorName.toLowerCase() + "%"));
            }


            // Status filtering
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }




    @Override
    public void markAsArrived(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Only booked appointments can be marked as arrived");
        }

        appointment.setStatus(AppointmentStatus.ARRIVED);
        appointmentRepository.save(appointment);
        log.info("✅ Appointment {} marked as ARRIVED", appointmentId);
    }



    @Override
    public void cancelAppointment(Long appointmentId) {
        log.info("🗑️ Cancelling appointment with id={}", appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Appointment already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        log.info("✅ Appointment cancelled: {}", appointmentId);
    }

    @Override
    public AppointmentResponseDto reschedule(Long id, AppointmentBookingDto dto) {
        log.info("♻️ Rescheduling appointment with id={}", id);

        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (existing.getStatus() == AppointmentStatus.CANCELLED) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Cannot reschedule a cancelled appointment");
        }
        if (existing.getStatus() == AppointmentStatus.COMPLETED) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Cannot reschedule a completed appointment");
        }

        existing.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(existing);
        log.info("🗑️ Existing appointment {} marked as CANCELLED", id);

        AppointmentBookingDto newBooking = AppointmentBookingDto.builder()
                .doctorId(dto.getDoctorId() != null ? dto.getDoctorId() : existing.getDoctor().getId())
                .patientId(dto.getPatientId() != null ? dto.getPatientId() : existing.getPatient().getId())
                .appointmentDate(dto.getAppointmentDate())
                .slotTime(dto.getSlotTime())
                .appointmentType(dto.getAppointmentType() != null ? dto.getAppointmentType() : existing.getType())
                .build();

        return bookAppointment(newBooking, existing); // ✅ track reschedule source
    }


    // 🔁 Utility method for finding users (doctor or patient)
    private User findUserOrThrow(Long userId, String type) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.NOT_FOUND, type + " not found"));
    }


    @Override
    public Page<DoctorScheduleDto> getDoctorSchedulesStructured(LocalDate date, String doctorName, String specialization, Pageable pageable) {
        DoctorSearchCriteria criteria = new DoctorSearchCriteria();
        criteria.setName(doctorName);
        criteria.setActive(true); // Assuming only active doctors are shown

        // Step 1: Use specification to fetch doctors from DB
        Specification<User> doctorSpec = new DoctorSpecification(criteria);
        Page<User> doctorPage = userRepository.findAll(doctorSpec, pageable);

        // Step 2: Build schedule DTOs
        List<DoctorScheduleDto> dtos = doctorPage.getContent().stream()
                .filter(doctor -> specialization == null ||
                        (doctor.getSpecialization() != null &&
                                doctor.getSpecialization().getName().equalsIgnoreCase(specialization)))
                .map(doctor -> {
                    List<Slot> slots = slotService.getEmergencySlots(doctor.getId(), date);
                    List<HourlySlotGroupDto> grouped = groupSlotsByHour(slots);
                    return DoctorScheduleDto.builder()
                            .id(doctor.getId())
                            .name("Dr. " + doctor.getFirstName() + " " + doctor.getLastName())
                            .specialization(doctor.getSpecialization() != null ? doctor.getSpecialization().getName() : "General")
                            .departmentId(doctor.getSpecialization() != null ?doctor.getSpecialization().getId() : 0)
                            .avatarUrl("https://storage.googleapis.com/uxpilot-auth.appspot.com/avatars/avatar-" + (doctor.getId() % 6 + 1) + ".jpg")
                            .timeSlots(grouped)
                            .build();
                })
                .toList();

        // Step 3: Return paginated result
        return new PageImpl<>(dtos, pageable, doctorPage.getTotalElements());
    }
    private List<HourlySlotGroupDto> groupSlotsByHour(List<Slot> slots) {
        // Group slots into 10-min segments by base hour
        Map<String, List<TimeSlotDto>> hourlyGrouped = new TreeMap<>();

        for (Slot slot : slots) {
            String hourLabel = slot.getStartTime().withMinute(0).toString(); // "09:00"
            String exactTime = slot.getStartTime().toString();              // "09:10"

            // ✅ Safe patient name using Optional
            String patientName = Optional.ofNullable(slot.getAppointment())
                    .map(Appointment::getPatient)
                    .map(p -> {
                        String first = Optional.ofNullable(p.getFirstName()).orElse("");
                        String last = Optional.ofNullable(p.getLastName()).orElse("");
                        return (first + " " + last).trim();
                    })
                    .filter(name -> !name.isBlank())
                    .orElse(slot.getStatus() == SlotStatus.LUNCH_BREAK ? "Lunch Break" : null);

            TimeSlotDto dto = TimeSlotDto.builder()
                    .time(exactTime)
                    .status(slot.getStatus())
                    .patientName(patientName)
                    .build();

            hourlyGrouped.computeIfAbsent(hourLabel, k -> new ArrayList<>()).add(dto);
        }

        return hourlyGrouped.entrySet().stream()
                .map(e -> HourlySlotGroupDto.builder()
                        .timeLabel(e.getKey())
                        .slots(e.getValue().stream()
                                .sorted(Comparator.comparing(TimeSlotDto::getTime))
                                .toList())
                        .build())
                .toList();
    }


    private void validateSlotConflicts(User doctor, User patient, LocalDate date, LocalTime time) {
        if (appointmentRepository.existsByDoctorAndAppointmentDateAndSlotTime(doctor, date, time)) {
            throw new HospitalAPIException(HttpStatus.CONFLICT, "Doctor already has an appointment in this slot");
        }

        if (appointmentRepository.existsByPatientAndAppointmentDateAndSlotTime(patient, date, time)) {
            throw new HospitalAPIException(HttpStatus.CONFLICT, "Patient already has an appointment in this slot");
        }
    }

}
