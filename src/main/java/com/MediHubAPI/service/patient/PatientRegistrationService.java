package com.MediHubAPI.service.patient;

import com.MediHubAPI.dto.patient.register.PatientRegisterRequest;
import com.MediHubAPI.dto.patient.register.PatientRegisterResponse;
import com.MediHubAPI.dto.patient.register.PatientRegisterData;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Role;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.GovtIdType;
import com.MediHubAPI.model.enums.MaritalStatus;
import com.MediHubAPI.model.enums.ReferrerType;
import com.MediHubAPI.model.enums.Sex;
import com.MediHubAPI.repository.RoleRepository;
import com.MediHubAPI.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PatientRegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PatientRegisterResponse register(PatientRegisterRequest request) {
        PatientRegisterRequest.Demographics demographics = request.getDemographics();
        if (demographics == null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "demographics are required");
        }

        if (userRepository.existsByMobileNumber(request.getPhone())) {
            throw new HospitalAPIException(HttpStatus.CONFLICT, "DUPLICATE", "patient/phone already exists");
        }

        String providedFileNo = trimToNull(demographics.getFileNo());
        if (providedFileNo != null && userRepository.existsByFileNo(providedFileNo)) {
            throw new HospitalAPIException(HttpStatus.CONFLICT, "DUPLICATE", "patient/fileNo already exists");
        }

        String hospitalId = generateHospitalId();
        String fileNo = providedFileNo != null ? providedFileNo : generateFileNo();
        String username = generateUsername(demographics.getFirstName(), demographics.getLastName(), request.getPhone());
        String email = resolveEmail(demographics.getEmail(), request.getPhone(), hospitalId);
        String encodedPassword = passwordEncoder.encode(generatePassword());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setEnabled(true);

        user.setTitle(trimToNull(demographics.getTitle()));
        user.setFirstName(demographics.getFirstName());
        user.setLastName(trimToNull(demographics.getLastName()));
        user.setHospitalId(hospitalId);
        user.setFileNo(fileNo);
        user.setCountryCode(defaultCountryCode(request.getCountryCode()));
        user.setMobileNumber(request.getPhone());
        user.setLandlineNumber(trimToNull(request.getLandline()));
        user.setAlternateContact(trimToNull(request.getAlternateContact()));

        user.setDateOfBirth(parseDate(demographics.getDobISO()));
        user.setAge(demographics.getAge());
        user.setSex(parseEnumOrNull(Sex.class, demographics.getGender(), "gender"));
        user.setMaritalStatus(parseEnumOrNull(MaritalStatus.class, demographics.getMaritalStatus(), "maritalStatus"));
        user.setMotherTongue(trimToNull(demographics.getMotherTongue()));

        user.setGovtIdType(parseEnumOrNull(GovtIdType.class, demographics.getGovtIdType(), "govtIdType"));
        user.setGovtIdNumber(trimToNull(demographics.getGovtIdNo()));

        PatientRegisterRequest.Referrer referrer = request.getReferrer();
        user.setReferrerType(parseEnumOrNull(ReferrerType.class, referrer == null ? null : referrer.getType(), "referrerType"));
        user.setReferrerName(referrer == null ? null : trimToNull(referrer.getName()));
        user.setReferrerNumber(referrer == null ? null : trimToNull(referrer.getPhone()));
        user.setReferrerEmail(referrer == null ? null : trimToNull(referrer.getEmail()));
        user.setMainComplaint(referrer == null ? null : trimToNull(referrer.getMainComplaint()));

        PatientRegisterRequest.Address address = request.getAddress();
        if (address != null) {
            user.setAddressLine1(trimToNull(address.getLine1()));
            user.setAddressArea(trimToNull(address.getArea()));
            user.setAddressCity(trimToNull(address.getCity()));
            user.setAddressPin(trimToNull(address.getPin()));
            user.setAddressState(trimToNull(address.getState()));
            user.setAddressCountry(trimToNull(address.getCountry()));
        }

        user.setBloodGroup(request.getBloodGroup());
        user.setFathersName(trimToNull(request.getFatherName()));
        user.setMothersName(trimToNull(request.getMotherName()));
        user.setSpousesName(trimToNull(request.getSpouseName()));
        user.setEducation(trimToNull(request.getEducation()));
        user.setOccupation(trimToNull(request.getOccupation()));
        user.setReligion(trimToNull(request.getReligion()));
        user.setBirthWeightValue(request.getBirthWeight());
        user.setBirthWeightUnit(request.getBirthWeight() != null ? "kg" : null);

        user.setInternational(Boolean.TRUE.equals(request.getIsInternational()));
        user.setNeedsAttention(request.getNotes() != null && Boolean.TRUE.equals(request.getNotes().getNeedsAttention()));
        user.setNotes(request.getNotes() != null ? trimToNull(request.getNotes().getText()) : null);

        if (StringUtils.hasText(request.getPhotoBase64())) {
            try {
                byte[] photo = Base64.getDecoder().decode(request.getPhotoBase64());
                user.setPhoto(photo);
                user.setPhotoContentType("image/*");
            } catch (IllegalArgumentException e) {
                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "photoBase64 is not valid base64");
            }
        }

        Role patientRole = roleRepository.findByName(ERole.PATIENT)
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "Patient role not configured"));
        user.getRoles().add(patientRole);

        User saved = userRepository.save(user);

        PatientRegisterData data = PatientRegisterData.builder()
                .id(saved.getId())
                .hospitalId(saved.getHospitalId())
                .fileNo(saved.getFileNo())
                .fullName(buildFullName(saved))
                .phone(formatPhone(saved.getCountryCode(), saved.getMobileNumber()))
                .international(Boolean.TRUE.equals(saved.getInternational()))
                .build();

        return PatientRegisterResponse.builder()
                .status(HttpStatus.CREATED.value())
                .data(data)
                .message("created")
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultCountryCode(String countryCode) {
        String cc = StringUtils.hasText(countryCode) ? countryCode.trim() : "+91";
        if (!cc.startsWith("+")) {
            cc = "+" + cc;
        }
        return cc;
    }

    private String formatPhone(String countryCode, String phone) {
        String cc = defaultCountryCode(countryCode);
        return cc + "-" + phone;
    }

    private String buildFullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(user.getTitle())) {
            sb.append(user.getTitle().trim()).append(' ');
        }
        if (StringUtils.hasText(user.getFirstName())) {
            sb.append(user.getFirstName().trim());
        }
        if (StringUtils.hasText(user.getLastName())) {
            sb.append(' ').append(user.getLastName().trim());
        }
        return sb.toString().trim();
    }

    private String generatePassword() {
        return "P@t" + Math.abs(ThreadLocalRandom.current().nextInt());
    }

    private String generateUsername(String firstName, String lastName, String phone) {
        String base = ((trimToEmpty(firstName) + "." + trimToEmpty(lastName)).replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
        if (!StringUtils.hasText(base)) {
            base = "patient";
        }
        String candidate;
        int attempt = 0;
        do {
            String suffix = phone != null ? phone : String.valueOf(System.nanoTime());
            candidate = base + "." + suffix;
            if (attempt++ > 0) {
                candidate += "." + attempt;
            }
        } while (userRepository.existsByUsername(candidate));
        return candidate;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveEmail(String requestedEmail, String phone, String hospitalId) {
        if (StringUtils.hasText(requestedEmail)) {
            return requestedEmail.trim();
        }
        String localPart = (phone != null ? phone : "patient") + "+" + hospitalId;
        String email = localPart + "@patient.local";
        int attempt = 0;
        while (userRepository.existsByEmail(email)) {
            email = localPart + "+" + (++attempt) + "@patient.local";
        }
        return email;
    }

    private LocalDate parseDate(String iso) {
        if (!StringUtils.hasText(iso)) {
            return null;
        }
        try {
            return LocalDate.parse(iso);
        } catch (DateTimeParseException e) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "dobISO must be a valid ISO date");
        }
    }

    private <E extends Enum<E>> E parseEnumOrNull(Class<E> type, String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String candidate = value.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        try {
            return Enum.valueOf(type, candidate);
        } catch (IllegalArgumentException e) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", fieldName + " is invalid");
        }
    }

    private String generateHospitalId() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String hid;
        int attempts = 0;
        do {
            String tail = String.format(Locale.ROOT, "%04d", Math.abs(ThreadLocalRandom.current().nextInt(10000)));
            hid = "HID" + year.substring(2) + tail;
        } while (userRepository.existsByHospitalId(hid) && attempts++ < 20);

        if (userRepository.existsByHospitalId(hid)) {
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "Unable to generate hospitalId");
        }
        return hid;
    }

    private String generateFileNo() {
        String fileNo;
        int attempts = 0;
        do {
            fileNo = "F" + Math.abs(ThreadLocalRandom.current().nextInt(1_000_000));
        } while (userRepository.existsByFileNo(fileNo) && attempts++ < 20);
        return fileNo;
    }
}
