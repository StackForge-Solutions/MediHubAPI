package com.MediHubAPI.service;

import com.MediHubAPI.dto.BulkPatchRequest;
import com.MediHubAPI.dto.DoctorServiceCreateRequest;
import com.MediHubAPI.dto.DoctorServiceResponse;
import com.MediHubAPI.dto.DoctorServiceUpdateRequest;
import org.springframework.data.domain.Page;

public interface DoctorServiceItemService {
    Page<DoctorServiceResponse> list(Long doctorId, int page, int size, String sort,
                                     String q, String status, String minPrice, String maxPrice);

    DoctorServiceResponse create(Long doctorId, DoctorServiceCreateRequest req);

    DoctorServiceResponse get(Long doctorId, Long serviceId);

   DoctorServiceResponse update(Long doctorId, Long serviceId, DoctorServiceUpdateRequest req);

    void delete(Long doctorId, Long serviceId);

    int bulkPatch(Long doctorId,  BulkPatchRequest req);
}
