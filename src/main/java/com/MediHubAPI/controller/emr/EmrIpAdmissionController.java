package com.MediHubAPI.controller.emr;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.MediHubAPI.dto.emr.IpAdmissionFetchResponse;
import com.MediHubAPI.dto.emr.IpAdmissionSaveRequest;
import com.MediHubAPI.dto.emr.IpAdmissionSaveResponse;
import com.MediHubAPI.service.emr.IpAdmissionService;

@RestController
@RequestMapping("/api/emr")
@RequiredArgsConstructor
@Slf4j
public class EmrIpAdmissionController {

    private final IpAdmissionService ipAdmissionService;

    @GetMapping("/appointments/{appointmentId}/ip-admission")
    public IpAdmissionFetchResponse getIpAdmission(@PathVariable Long appointmentId) {
        log.info("API call: getIpAdmission appointmentId={}", appointmentId);
        return ipAdmissionService.getIpAdmission(appointmentId);
    }

    @PostMapping("/appointments/{appointmentId}/ip-admission")
    public IpAdmissionSaveResponse saveIpAdmission(
            @PathVariable Long appointmentId,
            @Valid @RequestBody IpAdmissionSaveRequest request
    ) {
        log.info("API call: saveIpAdmission appointmentId={}", appointmentId);
        return ipAdmissionService.saveIpAdmission(appointmentId, request);
    }
}
