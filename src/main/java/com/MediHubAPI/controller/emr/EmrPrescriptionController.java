package com.MediHubAPI.controller.emr;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.emr.PrescriptionFetchResponse;
import com.MediHubAPI.dto.emr.PrescriptionSaveRequest;
import com.MediHubAPI.dto.emr.PrescriptionSaveResponse;
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


    @PostMapping("/appointments/{appointmentId}/prescription")
    public PrescriptionSaveResponse savePrescription(
            @PathVariable Long appointmentId,
            @RequestBody PrescriptionSaveRequest request
    ) {
        log.info("API call: savePrescription appointmentId={}", appointmentId);
        return prescriptionService.saveOrUpdate(appointmentId, request);
    }
    
    @GetMapping("/appointments/{appointmentId}/prescription")
    public DataResponse<PrescriptionFetchResponse> fetchPrescription(
            @PathVariable Long appointmentId
    ) {
        log.info("API call: fetchPrescription appointmentId={}", appointmentId);
        PrescriptionFetchResponse data = prescriptionService.getByAppointmentId(appointmentId);
        return new DataResponse<>(data);
    }


}
