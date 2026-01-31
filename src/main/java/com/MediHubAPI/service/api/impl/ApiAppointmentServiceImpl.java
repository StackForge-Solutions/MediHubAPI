package com.MediHubAPI.service.api.impl;

import com.MediHubAPI.dto.AppointmentBookingDto;
import com.MediHubAPI.dto.AppointmentResponseDto;
import com.MediHubAPI.dto.api.AppointmentConfirmRequest;
import com.MediHubAPI.dto.api.AppointmentConfirmResponse;
import com.MediHubAPI.dto.patient.register.PatientRegisterData;
import com.MediHubAPI.dto.patient.register.PatientRegisterRequest;
import com.MediHubAPI.dto.patient.register.PatientRegisterResponse;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.AppointmentStatus;
import com.MediHubAPI.model.enums.AppointmentType;
import com.MediHubAPI.model.enums.SlotStatus;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.api.ApiAppointmentService;
import com.MediHubAPI.service.AppointmentService;
import com.MediHubAPI.service.SlotService;
import com.MediHubAPI.service.patient.PatientRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiAppointmentServiceImpl implements ApiAppointmentService {

    private static final Set<String> WALKIN_ALLOWED_AUTHORITIES = Set.of(
            "ROLE_SUPER_ADMIN",
            "ROLE_ADMIN",
            "ROLE_HR_MANAGER",
            "ROLE_RECEPTIONIST",
            "ROLE_BILLING_CLERK",
            "ROLE_NURSE",
            "ROLE_DOCTOR"
    );

    private static final DateTimeFormatter ISO_OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final AppointmentService appointmentService;
    private final SlotService slotService;
    private final PatientRegistrationService patientRegistrationService;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public AppointmentConfirmResponse confirmBooking(AppointmentConfirmRequest request) {
        LocalDate date = parseDate(request.getDateISO());
        LocalTime time = parseTime(request.getTimeHHmm());

        if (request.getDoctorId() == null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "MISSING_CONTEXT", "Missing doctor/slot context.");
        }

        AppointmentConfirmRequest.Booking booking = request.getBooking();
        if (booking == null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "MISSING_CONTEXT", "Missing doctor/slot context.");
        }

        validateBookingNotes(booking.getNotes());
        ensureWalkInAllowed(booking.getIsWalkin());

        var slot = slotService.getSlotByDoctorAndTime(request.getDoctorId(), date, time);
        var requestedStatus = parseSlotStatus(request.getSlotStatus());

        if (slot.getStatus() != requestedStatus) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "SLOT_UNAVAILABLE", "Selected slot is already booked.");
        }

        if (slot.getStatus() == SlotStatus.BLOCKED) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "SLOT_BLOCKED", "Selected slot is blocked.");
        }

        if (slot.getAppointment() != null && slot.getAppointment().getStatus() != AppointmentStatus.CANCELLED) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "SLOT_UNAVAILABLE", "Selected slot is already booked.");
        }

        LocalDateTime slotDateTime = LocalDateTime.of(date, time);
        if (slotDateTime.isBefore(LocalDateTime.now())) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "PAST_SLOT", "Past time slots cannot be booked.");
        }

        PatientResolution patientResolution = resolvePatient(request.getPatient(), booking);

        AppointmentBookingDto bookingDto = AppointmentBookingDto.builder()
                .doctorId(request.getDoctorId())
                .patientId(patientResolution.getId())
                .appointmentDate(date)
                .slotTime(time)
                .appointmentType(resolveAppointmentType(booking))
                .build();

        AppointmentResponseDto booked;
        try {
            booked = appointmentService.bookAppointment(bookingDto);
        } catch (DataIntegrityViolationException ex) {
            throw new HospitalAPIException(HttpStatus.CONFLICT, "SLOT_RACE", "Slot was booked by someone else. Please refresh.");
        }

        if (Boolean.TRUE.equals(booking.getMarkArrived()) && booked.getId() != null) {
            appointmentService.markAsArrived(booked.getId());
        }

        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "MISSING_CONTEXT", "Missing doctor/slot context."));
        User patient = userRepository.findById(patientResolution.getId())
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_PATIENT", "Selected patient not found."));

        String statusLabel = Boolean.TRUE.equals(booking.getMarkArrived()) ? "ARRIVED" : "CONFIRMED";
        String createdAtIso = LocalDateTime.now(ZoneOffset.UTC).format(ISO_OFFSET_FORMATTER);

        AppointmentConfirmResponse.PatientResponse patientResponse = AppointmentConfirmResponse.PatientResponse.builder()
                .id(patient.getId())
                .hospitalId(patient.getHospitalId())
                .registrationId(patient.getHospitalId())
                .newPatientHospitalId(patientResolution.isCreated() ? patient.getHospitalId() : null)
                .fullName(buildFullName(patient))
                .phone(formatPatientPhone(patient.getCountryCode(), patient.getMobileNumber()))
                .needsAttention(Boolean.TRUE.equals(patient.getNeedsAttention()))
                .isInternational(Boolean.TRUE.equals(patient.getInternational()))
                .build();

        AppointmentConfirmResponse.DoctorResponse doctorResponse = AppointmentConfirmResponse.DoctorResponse.builder()
                .id(doctor.getId())
                .name(buildDoctorFullName(doctor))
                .speciality(doctor.getSpecialization() != null ? doctor.getSpecialization().getName() : null)
                .departmentId(doctor.getSpecialization() != null ? doctor.getSpecialization().getId() : null)
                .build();

        return AppointmentConfirmResponse.builder()
                .appointmentId(booked.getId() != null ? booked.getId().toString() : null)
                .tokenNo(resolveTokenNo(booked.getId()))
                .status(statusLabel)
                .smsStatus(null)
                .createdAtISO(createdAtIso)
                .patient(patientResponse)
                .doctor(doctorResponse)
                .build();
    }

    private AppointmentType resolveAppointmentType(AppointmentConfirmRequest.Booking booking) {
        if (Boolean.TRUE.equals(booking.getIsWalkin())) {
            return AppointmentType.WALKIN;
        }
        if (!StringUtils.hasText(booking.getVisitType())) {
            return AppointmentType.IN_PERSON;
        }
        String normalized = booking.getVisitType().trim().toUpperCase();
        return switch (normalized) {
            case "ONLINE" -> AppointmentType.ONLINE;
            case "EMERGENCY" -> AppointmentType.EMERGENCY;
            case "WALKIN" -> AppointmentType.WALKIN;
            default -> AppointmentType.IN_PERSON;
        };
    }

    private void validateBookingNotes(String notes) {
        if (notes != null && notes.length() > 200) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "NOTES_TOO_LONG", "Notes must be within 200 characters.");
        }
    }

    private void ensureWalkInAllowed(Boolean isWalkin) {
        if (!Boolean.TRUE.equals(isWalkin)) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new HospitalAPIException(HttpStatus.FORBIDDEN, "NOT_AUTHORIZED_WALKIN", "Walk-in slots can be booked only by ADMIN/STAFF.");
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (WALKIN_ALLOWED_AUTHORITIES.contains(authority.getAuthority())) {
                return;
            }
        }
        throw new HospitalAPIException(HttpStatus.FORBIDDEN, "NOT_AUTHORIZED_WALKIN", "Walk-in slots can be booked only by ADMIN/STAFF.");
    }

    private SlotStatus parseSlotStatus(String value) {
        if (!StringUtils.hasText(value)) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "slotStatus is required");
        }
        try {
            return SlotStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "slotStatus must be a valid slot state");
        }
    }

    private LocalDate parseDate(String iso) {
        try {
            return LocalDate.parse(iso);
        } catch (DateTimeParseException ex) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "dateISO must be a valid ISO date");
        }
    }

    private LocalTime parseTime(String hhmm) {
        try {
            return LocalTime.parse(hhmm);
        } catch (DateTimeParseException ex) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "timeHHmm must be a valid HH:mm");
        }
    }

    private PatientResolution resolvePatient(AppointmentConfirmRequest.PatientBlock patient, AppointmentConfirmRequest.Booking booking) {
        if (patient == null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "MISSING_PATIENT", "Select a patient or register a new one.");
        }
        if (patient.getId() != null && patient.getDetails() != null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "patientId must be null when registering a new patient");
        }
        if (patient.getId() != null) {
            User existing = userRepository.findById(patient.getId())
                    .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_PATIENT", "Selected patient not found."));
            return new PatientResolution(existing.getId(), existing.getHospitalId(), false);
        }
        if (patient.getDetails() == null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "MISSING_PATIENT", "Select a patient or register a new one.");
        }
        PatientRegisterRequest registerRequest = buildRegistrationRequest(patient, booking);
        PatientRegisterResponse response = patientRegistrationService.register(registerRequest);
        PatientRegisterData data = response.getData();
        if (data == null || data.getId() == null) {
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "UNKNOWN", "Failed to create patient record.");
        }
        return new PatientResolution(data.getId(), data.getHospitalId(), true);
    }

    private PatientRegisterRequest buildRegistrationRequest(AppointmentConfirmRequest.PatientBlock patient,
                                                            AppointmentConfirmRequest.Booking booking) {
        AppointmentConfirmRequest.PatientDetails details = patient.getDetails();
        if (details == null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "MISSING_PATIENT", "Select a patient or register a new one.");
        }
        String fullName = patient.getFullName();
        if (!StringUtils.hasText(fullName)) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "FIRST_NAME_REQUIRED", "First name is required for new patient.");
        }
        String normalizedPhone = normalizePhone(patient.getPhone());
        if (!isValidPhone(normalizedPhone)) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_PHONE", "Phone must be numeric (10-13 digits).");
        }
        var referrer = details.getReferrer();
        if (referrer != null && StringUtils.hasText(referrer.getPhone())) {
            String normalizedReferrer = normalizePhone(referrer.getPhone());
            if (!isValidPhone(normalizedReferrer)) {
                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_PHONE", "Phone must be numeric (10-13 digits).");
            }
            referrer.setPhone(normalizedReferrer);
        }
        boolean hasGovtType = StringUtils.hasText(details.getGovtIdType());
        boolean hasGovtNumber = StringUtils.hasText(details.getGovtIdNumber());
        if (hasGovtType ^ hasGovtNumber) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_GOVT_ID", "Government ID Type and Number must be provided together.");
        }

        PatientRegisterRequest request = new PatientRegisterRequest();
        request.setPhone(normalizedPhone);
        request.setCountryCode(patient.getCountryCode());
        request.setFullName(fullName);
        request.setIsInternational(patient.getIsInternational());
        request.setPhotoBase64(details.getPhotoBase64());

        PatientRegisterRequest.Demographics demographics = new PatientRegisterRequest.Demographics();
        String[] nameParts = fullName.trim().split("\\s+", 2);
        demographics.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            demographics.setLastName(nameParts[1]);
        }
        demographics.setDobISO(details.getDobISO());
        demographics.setGovtIdType(details.getGovtIdType());
        demographics.setGovtIdNo(details.getGovtIdNumber());
        request.setDemographics(demographics);

        if (details.getAddress() != null) {
            AppointmentConfirmRequest.PatientAddress address = details.getAddress();
            PatientRegisterRequest.Address dest = new PatientRegisterRequest.Address();
            dest.setLine1(address.getLine1());
            dest.setArea(address.getArea());
            dest.setCity(address.getCity());
            dest.setPin(address.getPin());
            dest.setState(address.getState());
            dest.setCountry(address.getCountry());
            request.setAddress(dest);
        }

        if (referrer != null) {
            PatientRegisterRequest.Referrer destReferrer = new PatientRegisterRequest.Referrer();
            destReferrer.setType(referrer.getType());
            destReferrer.setName(referrer.getName());
            destReferrer.setPhone(referrer.getPhone());
            destReferrer.setEmail(referrer.getEmail());
            destReferrer.setMainComplaint(referrer.getMainComplaint());
            request.setReferrer(destReferrer);
        }

        PatientRegisterRequest.Notes notes = new PatientRegisterRequest.Notes();
        Boolean needsAttention = details.getNeedsAttention();
        if (needsAttention == null && booking != null) {
            needsAttention = booking.getNeedsAttention();
        }
        notes.setNeedsAttention(Boolean.TRUE.equals(needsAttention));
        notes.setText(details.getNotes());
        request.setNotes(notes);

        return request;
    }

    private String normalizePhone(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private boolean isValidPhone(String digits) {
        return digits != null && digits.matches("\\d{10,13}");
    }

    private String formatPatientPhone(String countryCode, String mobileNumber) {
        if (StringUtils.hasText(countryCode) && StringUtils.hasText(mobileNumber)) {
            return countryCode + "-" + mobileNumber;
        }
        if (StringUtils.hasText(mobileNumber)) {
            return mobileNumber;
        }
        return null;
    }

    private String buildFullName(User user) {
        if (user == null) {
            return null;
        }
        String first = StringUtils.hasText(user.getFirstName()) ? user.getFirstName().trim() : "";
        String last = StringUtils.hasText(user.getLastName()) ? user.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        if (StringUtils.hasText(combined)) {
            return combined;
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return "Patient";
    }

    private String buildDoctorFullName(User doctor) {
        String name = buildFullName(doctor);
        if (!StringUtils.hasText(name)) {
            name = doctor.getUsername();
        }
        if (!StringUtils.hasText(name)) {
            name = "Doctor";
        }
        return "Dr. " + name;
    }

    private String resolveTokenNo(Long appointmentId) {
        if (appointmentId == null) {
            return null;
        }
        return invoiceRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId)
                .map(com.MediHubAPI.model.billing.Invoice::getToken)
                .map(Object::toString)
                .orElse(null);
    }

    @lombok.Getter
    private static final class PatientResolution {
        private final Long id;
        private final String hospitalId;
        private final boolean created;

        private PatientResolution(Long id, String hospitalId, boolean created) {
            this.id = id;
            this.hospitalId = hospitalId;
            this.created = created;
        }

    }
}
