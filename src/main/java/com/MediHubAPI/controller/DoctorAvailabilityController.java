package com.MediHubAPI.controller;

import com.MediHubAPI.dto.directory.ListResponse;
import com.MediHubAPI.dto.doctor.DoctorSessionsData;
import com.MediHubAPI.dto.doctor.DoctorSessionsResponse;
import com.MediHubAPI.dto.doctor.DoctorSlotDto;
import com.MediHubAPI.service.doctors.DoctorAvailabilityService;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public
class DoctorAvailabilityController {

    private final DoctorAvailabilityService availabilityService;

    public
    DoctorAvailabilityController(DoctorAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/doctors/{doctorId}/sessions")
    public
    ResponseEntity<DoctorSessionsResponse> sessions(@PathVariable @Positive Long doctorId) {
        DoctorSessionsData     data     = availabilityService.getSessions(doctorId);
        DoctorSessionsResponse response = new DoctorSessionsResponse();
        response.setStatus(HttpStatus.OK.value());
        response.setData(data);
        response.setMessage("ok");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slots")
    public
    ResponseEntity<ListResponse<DoctorSlotDto>> slots(
            @RequestParam("date") @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam("doctorId") @Positive Long doctorId,
            @RequestParam(value = "durationMin", required = false) Integer durationMin
                                                     ) {
        List<DoctorSlotDto> data = availabilityService.getSlots(doctorId, date, durationMin);
        return ResponseEntity.ok(ListResponse.<DoctorSlotDto>builder()
                                             .status(HttpStatus.OK.value())
                                             .data(data)
                                             .message("ok")
                                             .build());
    }
}
