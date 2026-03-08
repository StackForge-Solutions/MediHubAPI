package com.MediHubAPI.controller.pharmacy;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.pharmacy.PharmacyQueueItemDto;
import com.MediHubAPI.service.pharmacy.PharmacyQueueService;

@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
@Slf4j
public class PharmacyQueueController {

    private final PharmacyQueueService pharmacyQueueService;

    /**
     * GET /api/pharmacy/queue?date=2026-03-08
     * date optional -> defaults to today
     */
    @GetMapping("/queue")
    public ResponseEntity<?> queue(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "unwrap", required = false, defaultValue = "false") boolean unwrap
    ) {
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        log.info("API call: pharmacy queue date={}", effectiveDate);
        List<PharmacyQueueItemDto> items = pharmacyQueueService.getQueue(effectiveDate);
        if (items == null) {
            items = List.of();
        }

        // Most clients expect {\"data\": [...]}. Pass unwrap=true to get raw array.
        return unwrap ? ResponseEntity.ok(items) : ResponseEntity.ok(new DataResponse<>(items));
    }
}
