package com.MediHubAPI.controller.lab;

import com.MediHubAPI.dto.lab.LabTestDetailsDto;
import com.MediHubAPI.dto.lab.UpdateLabTestDetailsRequest;
import com.MediHubAPI.service.lab.LabTestDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/lab-tests")
@RequiredArgsConstructor
@Slf4j
public class LabTestDetailsController {

    private final LabTestDetailsService labTestDetailsService;

    @GetMapping("/{patientId}")
    public LabTestDetailsDto getLabTests(@PathVariable Long patientId) {
        log.info("API call: lab tests details patientId={}", patientId);
        return labTestDetailsService.getDetails(patientId);
    }

    @PutMapping
    public ResponseEntity<Void> updateLabTestDetails(@Valid @RequestBody UpdateLabTestDetailsRequest request,
                                                     @RequestHeader(name = "X-User", required = false) String user) {
        String authorizedBy = user == null ? "system" : user;
        log.info("API call: update lab tests details patientId={}, billNo={}", request.getPatientId(), request.getBillNo());
        labTestDetailsService.updateLabTestDetails(request, authorizedBy);
        return ResponseEntity.ok().build();
    }

}
