package com.MediHubAPI.service;


import com.MediHubAPI.dto.PatientCreateDto;
import com.MediHubAPI.dto.PatientDetailsDto;
import com.MediHubAPI.dto.PatientResponseDto;
import com.MediHubAPI.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PatientService {

    /**
     * Create patient with JSON + optional photo (same multipart request).
     */
    PatientResponseDto registerPatient(PatientCreateDto dto, MultipartFile photo);

    /**
     * Fetch a patient summary by id.
     */
    PatientResponseDto getPatient(Long id);

    /**
     * Update / replace the patient's photo.
     */
    void updatePatientPhoto(Long id, MultipartFile photo);

    /**
     * Get raw photo bytes + content type (for controller to return).
     */
    byte[] getPatientPhoto(Long id);

    /**
     * Get the stored photo’s content type, or null if none.
     */
    String getPatientPhotoContentType(Long id);

    User findById(Long id);


    List<PatientDetailsDto> getPatientsByDate(LocalDate date);

    List<PatientDetailsDto> getPatientsByWeek(LocalDate date);

    List<PatientDetailsDto> getPatientsByMonth(LocalDate date);

    List<PatientDetailsDto> getNextPatients();

    List<PatientDetailsDto> getPreviousPatients();
}
