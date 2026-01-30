package com.MediHubAPI.controller.patient;

import com.MediHubAPI.dto.patient.register.PatientRegisterRequest;
import com.MediHubAPI.dto.patient.register.PatientRegisterResponse;
import com.MediHubAPI.service.patient.PatientRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientRegistrationController {

    private final PatientRegistrationService patientRegistrationService;

    @PostMapping("/register")
    public ResponseEntity<PatientRegisterResponse> register(@Valid @RequestBody PatientRegisterRequest request) {
        PatientRegisterResponse response = patientRegistrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
