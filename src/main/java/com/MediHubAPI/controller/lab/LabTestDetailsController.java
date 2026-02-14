package com.MediHubAPI.controller.lab;

import com.MediHubAPI.dto.lab.LabTestDetailsDto;
import com.MediHubAPI.service.lab.LabTestDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
