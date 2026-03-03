package com.MediHubAPI.controller.lab;

import com.MediHubAPI.service.lab.LabQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lab-queue")
@RequiredArgsConstructor
@Slf4j
public class LabQueueController {

    private final LabQueueService labQueueService;

    @GetMapping
    public Map<String, Object> getLabQueue(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "status", defaultValue = "all") String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "room", required = false) String room,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        log.info("API call: lab queue date={}, status={}, search={}, room={}, page={}, pageSize={}",
                date, status, search, room, page, pageSize);

        var result = labQueueService.fetchLabQueuePage(date, status, search, room, page, pageSize);

        Map<String, Object> resp = new HashMap<>();
        resp.put("items", result.getContent());
        resp.put("lastRefreshedAt", Instant.now().toString());
        resp.put("page", result.getNumber());
        resp.put("pageSize", result.getSize());
        resp.put("totalItems", result.getTotalElements());
        resp.put("totalPages", result.getTotalPages());
        return resp;
    }
}
