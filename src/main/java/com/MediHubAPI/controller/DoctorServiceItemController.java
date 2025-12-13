package com.MediHubAPI.controller;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.service.DoctorServiceItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/doctors/{doctorId}/services")
public class DoctorServiceItemController {

    private final DoctorServiceItemService service;

    // GET /api/doctors/{doctorId}/services?q=&status=&minPrice=&maxPrice=&page=&size=&sort=
    @GetMapping
    public ResponseEntity<PageResponse< DoctorServiceResponse>> list(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,          // e.g., "updatedAt,DESC"
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,        // ACTIVE | INACTIVE
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice
    ) {
        Page<DoctorServiceResponse> p = service.list(doctorId, page, size, sort, q, status, minPrice, maxPrice);
        PageResponse<DoctorServiceResponse> body = PageResponse.<DoctorServiceResponse>builder()
                .content(p.getContent())
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .build();
        return ResponseEntity.ok(body);
    }

    // POST /api/doctors/{doctorId}/services
    @PostMapping
    public ResponseEntity<DoctorServiceResponse> create(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorServiceCreateRequest req
    ) {
        return ResponseEntity.status(201).body(service.create(doctorId, req));
    }

    // GET /api/doctors/{doctorId}/services/{serviceId}
    @GetMapping("/{serviceId}")
    public ResponseEntity< DoctorServiceResponse> get(
            @PathVariable Long doctorId, @PathVariable Long serviceId
    ) {
        return ResponseEntity.ok(service.get(doctorId, serviceId));
    }

    // PUT /api/doctors/{doctorId}/services/{serviceId}
    @PutMapping("/{serviceId}")
    public ResponseEntity< DoctorServiceResponse> update(
            @PathVariable Long doctorId, @PathVariable Long serviceId,
            @Valid @RequestBody DoctorServiceUpdateRequest req
    ) {
        return ResponseEntity.ok(service.update(doctorId, serviceId, req));
    }

    // DELETE /api/doctors/{doctorId}/services/{serviceId}
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long doctorId, @PathVariable Long serviceId
    ) {
        service.delete(doctorId, serviceId);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/doctors/{doctorId}/services/bulk
    @PatchMapping("/bulk")
    public ResponseEntity<String> bulkPatch(
            @PathVariable Long doctorId,
            @Valid @RequestBody BulkPatchRequest req
    ) {
        int affected = service.bulkPatch(doctorId, req);
        return ResponseEntity.ok("Updated " + affected + " items");
    }
}
