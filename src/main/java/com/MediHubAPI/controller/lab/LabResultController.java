package com.MediHubAPI.controller.lab;

import com.MediHubAPI.dto.lab.SaveLabResultRequest;
import com.MediHubAPI.service.lab.LabResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lab-results")
@RequiredArgsConstructor
@Slf4j
public class LabResultController {

    private final LabResultService labResultService;

    @PostMapping
    public ResponseEntity<Void> save(@Valid @RequestBody SaveLabResultRequest request,
                                     @RequestHeader(name = "X-User", required = false) String user) {
        String authorizedBy = user == null ? "system" : user;
        labResultService.saveResults(request, authorizedBy);
        return ResponseEntity.ok().build();
    }
}
