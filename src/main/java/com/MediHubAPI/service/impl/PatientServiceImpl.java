package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.PatientCreateDto;
import com.MediHubAPI.dto.PatientDetailsDto;
import com.MediHubAPI.dto.PatientResponseDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.exception.ResourceNotFoundException;
import com.MediHubAPI.model.*;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.PatientRepository;
import com.MediHubAPI.repository.SpecializationRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.PatientService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final UserRepository userRepository;
    private final SpecializationRepository specializationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;


    @Override
    public PatientResponseDto registerPatient(PatientCreateDto dto, MultipartFile photo) {
// 1) Validate consulting doctor FK
        User doctor = userRepository.findById(dto.getConsultingDoctorId())
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "Consulting doctor not found"));


        boolean isDoctor = doctor.getRoles().stream().map(Role::getName).anyMatch(r -> r == ERole.DOCTOR);
        if (!isDoctor) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Consulting user is not a doctor");
        }


        Specialization specialization = specializationRepository.findById(dto.getSpecializationId())
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "Specialization not found"));


// 2) Create the PATIENT as a row in users
        User patient = new User();
        patient.setUsername(generatePatientUsername(dto));
        patient.setEmail(dto.getEmail());
        patient.setPassword(passwordEncoder.encode(dto.getPassword())); // if applicable

        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setEnabled(true);


// assign PATIENT role via your RoleService/Repository if required elsewhere
// e.g., roleRepository.findByName(ERole.PATIENT) and add to patient.getRoles()


// Map patient-specific fields
        patient.setHospitalId(generateUniqueHospitalId());
        patient.setMobileNumber(dto.getMobileNumber());
        patient.setAlternateContact(dto.getAlternateContact());
        patient.setLandlineNumber(dto.getLandlineNumber());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setSex(dto.getSex());
        patient.setMaritalStatus(dto.getMaritalStatus());
        patient.setGovtIdType(dto.getGovtIdType());
        patient.setGovtIdNumber(dto.getGovtIdNumber());
        patient.setOtherHospitalIds(dto.getOtherHospitalIds());
        patient.setMotherTongue(dto.getMotherTongue());
        patient.setReferrerType(dto.getReferrerType());
        patient.setReferrerName(dto.getReferrerName());
        patient.setReferrerNumber(dto.getReferrerNumber());
        patient.setReferrerEmail(dto.getReferrerEmail());
        patient.setConsultingDoctor(doctor);
        patient.setSpecialization(specialization);
        patient.setMainComplaint(dto.getMainComplaint());
        patient.setBloodGroup(dto.getBloodGroup());
        patient.setFathersName(dto.getFathersName());
        patient.setMothersName(dto.getMothersName());
        patient.setSpousesName(dto.getSpousesName());
        patient.setEducation(String.valueOf(dto.getEducation()));
        patient.setOccupation(String.valueOf(dto.getOccupation()));
        patient.setReligion(String.valueOf(dto.getReligion()));
        patient.setBirthWeightValue(dto.getBirthWeightValue());
        patient.setBirthWeightUnit(normalizeUnit(dto.getBirthWeightUnit()));


        if (photo != null && !photo.isEmpty()) {
            try {
                patient.setPhoto(photo.getBytes());
                patient.setPhotoContentType(photo.getContentType());
            } catch (Exception e) {
                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Invalid photo upload");
            }
        }


        patient = userRepository.save(patient);
        return toResponseDto(patient);
    }

//    @Override
//    @Transactional
//    public PatientResponseDto registerPatient(PatientCreateDto dto, MultipartFile photo) {
//        // 1) Validate consulting doctor FK
//        User doctor = userRepository.findById(dto.getConsultingDoctorId())
//                .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "Consulting doctor not found"));
//
//
//        Specialization specialization = specializationRepository.findById(dto.getSpecializationId())
//                .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "Specialization not found"));
//
//        // Optional: ensure the user has DOCTOR role
//        boolean isDoctor = doctor.getRoles().stream().map(Role::getName).anyMatch(r -> r == ERole.DOCTOR);
//        if (!isDoctor) {
//            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Consulting user is not a doctor");
//        }
//
//        // 2) Create/associate backing User for patient (if you need auth later)
//        //    If you already create Users elsewhere, adapt this part accordingly.
//        User patientUser = new User();
//        patientUser.setUsername(generatePatientUsername(dto));
//        patientUser.setEmail(dto.getEmail());
//        patientUser.setFirstName(dto.getFirstName());
//        patientUser.setLastName(dto.getLastName());
//        patientUser.setEnabled(true);
//        // Assign PATIENT role in your user creation flow if required
//        patientUser = userRepository.save(patientUser);
//
//        // 3) Build Patient entity
//        Patient patient = Patient.builder()
//                .user(patientUser)
//                .hospitalId(generateUniqueHospitalId())
//                .firstName(dto.getFirstName())
//                .lastName(dto.getLastName())
//                .mobileNumber(dto.getMobileNumber())
//                .alternateContact(dto.getAlternateContact())
//                .landlineNumber(dto.getLandlineNumber())
//                .dateOfBirth(dto.getDateOfBirth())
//                .sex(dto.getSex())
//                .maritalStatus(dto.getMaritalStatus())
//                .email(dto.getEmail())
//                .govtIdType(dto.getGovtIdType())
//                .govtIdNumber(dto.getGovtIdNumber())
//                .otherHospitalIds(dto.getOtherHospitalIds())
//                .motherTongue(dto.getMotherTongue())
//                .referrerType(dto.getReferrerType())
//                .referrerName(dto.getReferrerName())
//                .referrerNumber(dto.getReferrerNumber())
//                .referrerEmail(dto.getReferrerEmail())
//                .consultingDoctor(doctor)
//                .specialization(specialization)
//                .mainComplaint(dto.getMainComplaint())
//                .bloodGroup(dto.getBloodGroup())
//                .fathersName(dto.getFathersName())
//                .mothersName(dto.getMothersName())
//                .spousesName(dto.getSpousesName())
//                .education(dto.getEducation())
//                .occupation(dto.getOccupation())
//                .religion(dto.getReligion())
//                .birthWeightValue(dto.getBirthWeightValue())
//                .birthWeightUnit(normalizeUnit(dto.getBirthWeightUnit()))
//                .build();
//
//        // 4) Photo (optional)
//        if (photo != null && !photo.isEmpty()) {
//            try {
//                patient.setPhoto(photo.getBytes());
//                patient.setPhotoContentType(photo.getContentType());
//            } catch (Exception e) {
//                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Invalid photo upload");
//            }
//        }
//
//        patient = userRepository.save(patient);
//        return toResponseDto(patient);
//    }


    @Override
    @Transactional
    public void updatePatientPhoto(Long id, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "No photo provided");
        }
        User p = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        try {
            p.setPhoto(photo.getBytes());
            p.setPhotoContentType(photo.getContentType());
        } catch (Exception e) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Invalid photo upload");
        }
        userRepository.save(p);
    }

    @Override
    public PatientResponseDto getPatient(Long id) {
        User p = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toResponseDto(p);
    }


    @Override
    public byte[] getPatientPhoto(Long id) {
        User p = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        if (p.getPhoto() == null) {
            throw new ResourceNotFoundException("PatientPhoto", "patientId", id);
        }
        return p.getPhoto();
    }

    @Override
    public String getPatientPhotoContentType(Long id) {
        User p = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return p.getPhotoContentType();
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }
    @Override
    public List<PatientDetailsDto> getPatientsByDate(LocalDate date) {
        return appointmentRepository.findPatientsByDate(date);
    }
    @Override
    public List<PatientDetailsDto> getPatientsByWeek(LocalDate date) {
        LocalDate start = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate end = date.with(java.time.DayOfWeek.SUNDAY);
        return appointmentRepository.findPatientsByDateRange(start, end);
    }
    @Override
    public List<PatientDetailsDto> getPatientsByMonth(LocalDate date) {
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = date.with(TemporalAdjusters.lastDayOfMonth());
        return appointmentRepository.findPatientsByDateRange(start, end);
    }
    @Override
    public List<PatientDetailsDto> getNextPatients() {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findPatientsByDateRange(today.plusDays(1), today.plusDays(30));
    }
    @Override
    public List<PatientDetailsDto> getPreviousPatients() {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findPatientsByDateRange(today.minusDays(30), today.minusDays(1));
    }

    // ------------------- helpers -------------------

    // In PatientServiceImpl (or wherever the helper lives)
    private PatientResponseDto toResponseDto(User p) {
        // Build consulting doctor full name (null-safe)
        String doctorName = null;
        if (p.getConsultingDoctor() != null) {
            User d = p.getConsultingDoctor();
            String fn = d.getFirstName() != null ? d.getFirstName() : "";
            String ln = d.getLastName() != null ? d.getLastName() : "";
            doctorName = (fn + " " + ln).trim();
        }

        // Specialization display (prefer name, not toString)
        String specializationName = null;
        if (p.getSpecialization() != null) {
            specializationName = p.getSpecialization().getName(); // adjust if your field differs
        }

        return PatientResponseDto.builder()
                .id(p.getId())
                .hospitalId(p.getHospitalId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .mobileNumber(p.getMobileNumber())
                .email(p.getEmail())
                .specializationName(specializationName)
                .consultingDoctorName(doctorName)
                .photoContentType(p.getPhotoContentType())
                .photoPresent(p.getPhoto() != null && p.getPhoto().length > 0)
                .build();
    }

//    private PatientResponseDto toResponseDto(Patient patient) {
//        String doctorName = null;
//        if (patient.getConsultingDoctor() != null) {
//            User d = patient.getConsultingDoctor();
//            doctorName = ((d.getFirstName() != null ? d.getFirstName() : "") + " " +
//                    (d.getLastName() != null ? d.getLastName() : "")).trim();
//        }
//
//        return PatientResponseDto.builder()
//                .id(patient.getId())
//                .hospitalId(patient.getHospitalId())
//                .firstName(patient.getFirstName())
//                .lastName(patient.getLastName())
//                .mobileNumber(patient.getMobileNumber())
//                .email(patient.getEmail())
//                .specializationName(patient.getSpecialization() != null ? String.valueOf(patient.getSpecialization()) : null)
//                .consultingDoctorName(doctorName)
//                .photoContentType(patient.getPhotoContentType())
//                .photoPresent(patient.getPhoto() != null && patient.getPhoto().length > 0)
//                .build();
//    }

    private String normalizeUnit(String unit) {
        if (!StringUtils.hasText(unit)) return null;
        String u = unit.trim().toLowerCase(Locale.ROOT);
        return switch (u) {
            case "kg", "g", "lb" -> u;
            default -> null;
        };
    }

    /**
     * Create a unique hospital id like PAT-2025-1234
     */
    private String generateUniqueHospitalId() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String hid;
        int maxAttempts = 10;
        int attempts = 0;
        do {
            String tail = String.format(Locale.ROOT, "%04d",
                    Math.abs((int) (System.nanoTime() % 10000)));
            hid = "PAT-" + year + "-" + tail;
            attempts++;
        } while (userRepository.existsByHospitalId(hid) && attempts < maxAttempts);

        if (userRepository.existsByHospitalId(hid)) {
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate hospital ID");
        }
        return hid;
    }

    private String generatePatientUsername(PatientCreateDto dto) {
        String base = ((dto.getFirstName() == null ? "" : dto.getFirstName()) + "." +
                (dto.getLastName() == null ? "" : dto.getLastName()))
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        return base + "." + System.currentTimeMillis();
    }

}