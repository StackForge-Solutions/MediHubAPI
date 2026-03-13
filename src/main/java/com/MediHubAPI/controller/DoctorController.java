package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.DoctorAvailabilityDto;
import com.MediHubAPI.dto.DoctorProfileDto;
import com.MediHubAPI.dto.DoctorSearchCriteria;
import com.MediHubAPI.dto.SpecializationOptionDto;
import com.MediHubAPI.dto.SlotResponseDto;
import com.MediHubAPI.dto.UserDto;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            DoctorSearchCriteria criteria) {
        validatePagination(page, size);
        log.info("Fetching doctors with criteria={} page={} size={}", criteria, page, size);
        Page<UserDto> result = doctorService.searchDoctors(criteria, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result, "/doctors", "Doctors fetched successfully"));
    }

    @GetMapping("/specializations")
    public ResponseEntity<ApiResponse<List<SpecializationOptionDto>>> getSpecializations() {
        List<SpecializationOptionDto> specializations = doctorService.getSpecializations();
        return ResponseEntity.ok(
                ApiResponse.success(specializations, "/doctors/specializations", "Specializations fetched successfully")
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorProfileDto>> getDoctorProfile(@PathVariable Long id) {
        log.info("Fetching doctor profile for ID={}", id);
        DoctorProfileDto dto = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "/doctors/" + id, "Doctor profile fetched successfully"));
    }

    @PostMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<Void>> defineAvailability(
            @PathVariable Long id,
            @Valid @RequestBody DoctorAvailabilityDto dto) {
        log.info("Defining availability for doctor ID={} with data={}", id, dto);
        doctorService.defineAvailability(id, dto);
        return ResponseEntity.ok(
                ApiResponse.ok("Availability defined successfully", "/doctors/" + id + "/availability"));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<Void>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody DoctorAvailabilityDto dto) {
        log.info("Updating availability for doctor ID={} with data={}", id, dto);
        doctorService.updateAvailability(id, dto);
        return ResponseEntity.ok(
                ApiResponse.ok("Availability updated successfully", "/doctors/" + id + "/availability"));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<ApiResponse<List<SlotResponseDto>>> getSlotsByDate(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Fetching slots for doctor ID={} on date={}", id, date);
        List<SlotResponseDto> slots = doctorService.getSlotsForDate(id, date);
        return ResponseEntity.ok(
                ApiResponse.success(slots, "/doctors/" + id + "/slots?date=" + date, "Slots fetched successfully"));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateDoctor(@PathVariable Long id) {
        log.info("Deactivating doctor with ID={}", id);
        doctorService.deactivateDoctor(id);
        return ResponseEntity.ok(ApiResponse.ok("Doctor deactivated successfully", "/doctors/" + id + "/deactivate"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable Long id) {
        log.info("Deleting doctor with ID={}", id);
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(ApiResponse.ok("Doctor deleted successfully", "/doctors/" + id));
    }

    private void validatePagination(int page, int size) {
        List<ValidationException.ValidationErrorDetail> details = new ArrayList<>();
        if (page < 0) {
            details.add(new ValidationException.ValidationErrorDetail(
                    "page",
                    "page must be greater than or equal to 0"
            ));
        }
        if (size < 1) {
            details.add(new ValidationException.ValidationErrorDetail(
                    "size",
                    "size must be greater than or equal to 1"
            ));
        }
        if (!details.isEmpty()) {
            throw new ValidationException("Validation failed", details);
        }
    }
}
