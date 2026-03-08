package com.MediHubAPI.service.emr.impl;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.MediHubAPI.dto.emr.IpAdmissionFetchResponse;
import com.MediHubAPI.dto.emr.IpAdmissionSaveRequest;
import com.MediHubAPI.dto.emr.IpAdmissionSaveResponse;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.emr.IpAdmission;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.emr.IpAdmissionRepository;
import com.MediHubAPI.service.emr.IpAdmissionService;

@Service
@RequiredArgsConstructor
public class IpAdmissionServiceImpl implements IpAdmissionService {

    private static final DateTimeFormatter ISO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final AppointmentRepository appointmentRepository;
    private final IpAdmissionRepository ipAdmissionRepository;

    @Transactional(readOnly = true)
    @Override
    public IpAdmissionFetchResponse getIpAdmission(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new HospitalAPIException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment not found with ID: " + appointmentId
                ));

        return ipAdmissionRepository.findByAppointment_Id(appointmentId)
                .map(this::toFetchResponse)
                .orElseGet(() -> IpAdmissionFetchResponse.builder()
                        .ipAdmissionId(null)
                        .appointmentId(appointmentId)
                        .visitDate(appointment.getAppointmentDate())
                        .admissionAdvised("na")
                        .remarks(null)
                        .admissionReason(null)
                        .tentativeStayDays(null)
                        .notes(null)
                        .savedAt(null)
                        .build());
    }

    @Transactional
    @Override
    public IpAdmissionSaveResponse saveIpAdmission(Long appointmentId, IpAdmissionSaveRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new HospitalAPIException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment not found with ID: " + appointmentId
                ));

        if (request == null) {
            throw new HospitalAPIException(
                    HttpStatus.BAD_REQUEST,
                    "PAYLOAD_REQUIRED",
                    "Request body is required"
            );
        }

        if (request.getVisitDate() == null) {
            throw new HospitalAPIException(
                    HttpStatus.BAD_REQUEST,
                    "VISIT_DATE_REQUIRED",
                    "visitDate is required"
            );
        }

        String admissionAdvisedRaw = request.getAdmissionAdvised();
        if (admissionAdvisedRaw == null || admissionAdvisedRaw.trim().isEmpty()) {
            throw new HospitalAPIException(
                    HttpStatus.BAD_REQUEST,
                    "ADMISSION_ADVISED_REQUIRED",
                    "admissionAdvised is required"
            );
        }

        String admissionAdvised = admissionAdvisedRaw.trim().toLowerCase(Locale.ROOT);

        if (!"yes".equals(admissionAdvised) && !"no".equals(admissionAdvised) && !"na".equals(admissionAdvised)) {
            throw new HospitalAPIException(
                    HttpStatus.BAD_REQUEST,
                    "ADMISSION_ADVISED_INVALID",
                    "admissionAdvised must be one of: yes, no, na"
            );
        }

        if ("yes".equals(admissionAdvised)) {
            if (isBlank(request.getAdmissionReason())) {
                throw new HospitalAPIException(
                        HttpStatus.BAD_REQUEST,
                        "ADMISSION_REASON_REQUIRED",
                        "admissionReason is required when admissionAdvised is 'yes'"
                );
            }
            if (request.getTentativeStayDays() == null) {
                throw new HospitalAPIException(
                        HttpStatus.BAD_REQUEST,
                        "TENTATIVE_STAY_REQUIRED",
                        "tentativeStayDays is required when admissionAdvised is 'yes'"
                );
            }
            if (request.getTentativeStayDays() < 1) {
                throw new HospitalAPIException(
                        HttpStatus.BAD_REQUEST,
                        "TENTATIVE_STAY_MIN",
                        "tentativeStayDays must be at least 1"
                );
            }
        }

        IpAdmission ipAdmission = ipAdmissionRepository.findByAppointment_Id(appointmentId)
                .orElseGet(() -> IpAdmission.builder().appointment(appointment).build());

        ipAdmission.setVisitDate(request.getVisitDate());
        ipAdmission.setAdmissionAdvised(admissionAdvised);
        ipAdmission.setRemarks(request.getRemarks());
        ipAdmission.setAdmissionReason("yes".equals(admissionAdvised) ? request.getAdmissionReason() : null);
        ipAdmission.setTentativeStayDays("yes".equals(admissionAdvised) ? request.getTentativeStayDays() : null);
        ipAdmission.setNotes(request.getNotes());

        IpAdmission saved = ipAdmissionRepository.save(ipAdmission);

        String savedAtIso = saved.getSavedAt() == null
                ? null
                : saved.getSavedAt().atOffset(ZoneOffset.UTC).format(ISO_TS);

        return IpAdmissionSaveResponse.builder()
                .ipAdmissionId(saved.getId())
                .appointmentId(appointmentId)
                .savedAt(savedAtIso)
                .build();
    }

    private IpAdmissionFetchResponse toFetchResponse(IpAdmission ipAdmission) {
        String savedAtIso = ipAdmission.getSavedAt() == null
                ? null
                : ipAdmission.getSavedAt().atOffset(ZoneOffset.UTC).format(ISO_TS);

        return IpAdmissionFetchResponse.builder()
                .ipAdmissionId(ipAdmission.getId())
                .appointmentId(ipAdmission.getAppointment().getId())
                .visitDate(ipAdmission.getVisitDate())
                .admissionAdvised(ipAdmission.getAdmissionAdvised())
                .remarks(ipAdmission.getRemarks())
                .admissionReason(ipAdmission.getAdmissionReason())
                .tentativeStayDays(ipAdmission.getTentativeStayDays())
                .notes(ipAdmission.getNotes())
                .savedAt(savedAtIso)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
