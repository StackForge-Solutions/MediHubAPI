package com.MediHubAPI.controller.emr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.MediHubAPI.dto.emr.SummaryPrintDto;
import com.MediHubAPI.service.emr.SummaryPrintService;

@RestController
@RequestMapping("/api/emr")
@RequiredArgsConstructor
@Slf4j
public class VisitSummaryPrintController {

    private final SummaryPrintService summaryPrintService;

    @GetMapping("/appointments/{appointmentId}/summary-print")
    public ResponseEntity<SummaryPrintDto> summaryPrint(@PathVariable Long appointmentId) {
        log.info("API call: summary-print appointmentId={}", appointmentId);
        SummaryPrintDto dto = summaryPrintService.buildSummary(appointmentId);
        return ResponseEntity.ok(dto);
    }
}
