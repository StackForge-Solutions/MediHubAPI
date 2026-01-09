package com.MediHubAPI.controller.lab;

import com.MediHubAPI.dto.lab.LabTestMasterResponse;
import com.MediHubAPI.service.lab.LabTestMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/lab/tests")
@RequiredArgsConstructor
@Slf4j
public class LabTestMasterController {

    private final LabTestMasterService service;

    @GetMapping("/master")
    public ResponseEntity<LabTestMasterResponse> master(
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "departmentId", required = false) Long departmentId, // ignored for now
            @RequestParam(value = "priceListId", required = false) Long priceListId,   // ignored for now
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        LabTestMasterResponse resp = service.getMaster(active, q, limit, offset, sort);

        // Fill meta extras (traceId optional; timestamp already set in service)
        if (resp.getMeta() != null) {
            resp.getMeta().setTraceId(generateTraceId());
            resp.getMeta().setTimestamp(Instant.now().toString());
        }

        // ETag based on request+result size (cheap). If you add updatedAt later, use MAX(updatedAt) for stable ETag.
        String etag = weakEtag(active, q, limit, offset, sort, resp.getMeta() == null ? 0 : resp.getMeta().getCount());

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePrivate())
                .body(resp);
    }

    private String generateTraceId() {
        return Long.toHexString(System.nanoTime());
    }

    private String weakEtag(Boolean active, String q, Integer limit, Integer offset, String sort, int count) {
        try {
            String raw = String.valueOf(active) + "|" + q + "|" + limit + "|" + offset + "|" + sort + "|" + count;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return "W/\"" + sb + "\"";
        } catch (Exception e) {
            return "W/\"na\"";
        }
    }
}
