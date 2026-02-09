package com.MediHubAPI.controller.patient;

import com.MediHubAPI.dto.patient.register.PatientRegisterRequest;
import com.MediHubAPI.dto.patient.register.PatientRegisterResponse;
import com.MediHubAPI.dto.patient.search.PatientSearchBy;
import com.MediHubAPI.dto.patient.search.PatientSearchResponse;
import com.MediHubAPI.dto.patient.search.PatientSearchResultDto;
import com.MediHubAPI.service.patient.PatientRegistrationService;
import com.MediHubAPI.service.patient.PatientSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientRegistrationController {

    private final PatientRegistrationService patientRegistrationService;
    private final PatientSearchService patientSearchService;

    @PostMapping("/register")
    public ResponseEntity<PatientRegisterResponse> register(@Valid @RequestBody PatientRegisterRequest request) {
        PatientRegisterResponse response = patientRegistrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<PatientSearchResponse> search(
            @RequestParam("by") String by,
            @RequestParam("q") String query
    ) {
        PatientSearchBy searchBy = PatientSearchBy.from(by);
        java.util.List<PatientSearchResultDto> data = patientSearchService.search(searchBy, query);
        PatientSearchResponse response = PatientSearchResponse.builder()
                .status(HttpStatus.OK.value())
                .data(data)
                .message("ok")
                .build();
        return ResponseEntity.ok(response);
    }
}
