package com.MediHubAPI.controller.emr;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.emr.PrescriptionFetchResponse;
import com.MediHubAPI.dto.emr.PrescriptionSaveRequest;
import com.MediHubAPI.dto.emr.PrescriptionSaveResponse;
import com.MediHubAPI.dto.emr.importprev.PreviousPrescriptionsDataDto;
import com.MediHubAPI.service.emr.EmrPreviousPrescriptionService;
import com.MediHubAPI.service.emr.PrescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emr")
@RequiredArgsConstructor
@Slf4j
public class EmrPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final EmrPreviousPrescriptionService service;


    @PostMapping("/appointments/{appointmentId}/prescription")
    public PrescriptionSaveResponse savePrescription(
            @PathVariable Long appointmentId,
            @RequestBody PrescriptionSaveRequest request
    ) {
        log.info("API call: savePrescription appointmentId={}", appointmentId);
        return prescriptionService.saveOrUpdate(appointmentId, request);
    }
    
    @GetMapping("/appointments/{appointmentId}/previous")
    public DataResponse<PrescriptionFetchResponse> fetchPrescription(
            @PathVariable Long appointmentId
    ) {
        log.info("API call: fetchPrescription appointmentId={}", appointmentId);
        PrescriptionFetchResponse data = prescriptionService.getByAppointmentId(appointmentId);
        return new DataResponse<>(data);
    }


    /** Option A: By patient */
    @GetMapping("/patients/{patientId}/previous-prescriptions")
    public DataResponse<PreviousPrescriptionsDataDto> byPatient(
            @PathVariable Long patientId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        log.info("API call: previous prescriptions by patientId={}, limit={}", patientId, limit);
        return new DataResponse<>(service.byPatient(patientId, limit));
    }

    /** Option B: By appointment (previous before this appointment) */
    @GetMapping("/appointments/{appointmentId}/previous-prescriptions")
    public DataResponse<PreviousPrescriptionsDataDto> byAppointment(
            @PathVariable Long appointmentId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        log.info("API call: previous prescriptions by appointmentId={}, limit={}", appointmentId, limit);
        return new DataResponse<>(service.byAppointment(appointmentId, limit));
    }

}
