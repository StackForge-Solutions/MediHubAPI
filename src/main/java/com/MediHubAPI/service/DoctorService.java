package com.MediHubAPI.service;

import com.MediHubAPI.dto.*;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.List;

public interface DoctorService {
    Page<UserDto> searchDoctors(DoctorSearchCriteria criteria, Pageable pageable);
    List<SpecializationOptionDto> getSpecializations();
    DoctorProfileDto getDoctorById(Long id);
    void defineAvailability(Long id, DoctorAvailabilityDto dto);
    void updateAvailability(Long id, DoctorAvailabilityDto dto);
    List<SlotResponseDto> getSlotsForDate(Long id, LocalDate date);
    void deactivateDoctor(Long id);
    void deleteDoctor(Long id);
}
