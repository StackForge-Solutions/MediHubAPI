package com.MediHubAPI.controller.emr;

import com.MediHubAPI.dto.emr.EmrQueueItemDto;
import com.MediHubAPI.service.emr.EmrQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emr")
public class EmrQueueController {

    private final EmrQueueService emrQueueService;

    /**
     * GET /api/v1/emr/queue?date=2026-01-05&doctorId=2
     * date optional -> defaults to today
     * doctorId optional -> all doctors
     */
    @GetMapping("/queue")
    public ResponseEntity<List<EmrQueueItemDto>> queue(
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        return ResponseEntity.ok(emrQueueService.getQueue(effectiveDate, doctorId));
    }
}
