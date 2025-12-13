package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.BulkPatchRequest;
import com.MediHubAPI.dto.DoctorServiceCreateRequest;
import com.MediHubAPI.dto.DoctorServiceResponse;
import com.MediHubAPI.dto.DoctorServiceUpdateRequest;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.billing.DoctorServiceItem;
import com.MediHubAPI.model.enums.ServiceStatus;
import com.MediHubAPI.repository.DoctorServiceItemRepository;
import com.MediHubAPI.repository.specs.DoctorServiceItemSpecs;
import com.MediHubAPI.service.DoctorServiceItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorServiceItemServiceImpl implements DoctorServiceItemService {

    private final DoctorServiceItemRepository repo;

    // helper to create a lightweight reference to doctor
    private User refDoctor(Long doctorId) {
        User u = new User();
        u.setId(doctorId);
        return u;
    }

    // map entity to DTO
    private DoctorServiceResponse map(DoctorServiceItem e) {
        return DoctorServiceResponse.builder()
                .id(e.getId())
                .doctorId(e.getDoctor().getId())
                .name(e.getName())
                .price(e.getPrice())
                .status(e.getStatus())
                .code(e.getCode())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    @Override
    public Page<DoctorServiceResponse> list(Long doctorId, int page, int size, String sort,
                                            String q, String statusStr, String minPriceStr, String maxPriceStr) {
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String prop = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && "DESC".equalsIgnoreCase(parts[1])) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortObj = Sort.by(dir, prop);
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sortObj);

        ServiceStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            status = ServiceStatus.valueOf(statusStr.toUpperCase());
        }

        BigDecimal minPrice = (minPriceStr == null || minPriceStr.isBlank()) ? null : new BigDecimal(minPriceStr);
        BigDecimal maxPrice = (maxPriceStr == null || maxPriceStr.isBlank()) ? null : new BigDecimal(maxPriceStr);

        Specification<DoctorServiceItem> spec = Specification
                .where(DoctorServiceItemSpecs.forDoctor(doctorId))
                .and(DoctorServiceItemSpecs.nameLike(q))
                .and(DoctorServiceItemSpecs.withStatus(status))
                .and(DoctorServiceItemSpecs.minPrice(minPrice))
                .and(DoctorServiceItemSpecs.maxPrice(maxPrice));

        Page<DoctorServiceItem> pageData = repo.findAll(spec, pageable);
        return pageData.map(this::map);
    }

    @Override
    public DoctorServiceResponse create(Long doctorId, DoctorServiceCreateRequest req) {
        if (repo.existsByDoctor_IdAndNameIgnoreCase(doctorId, req.getName())) {
            throw new DataIntegrityViolationException("Duplicate service name for this doctor");
        }

        DoctorServiceItem e = DoctorServiceItem.builder()
                .doctor(refDoctor(doctorId))
                .name(req.getName().trim())
                .price(req.getPrice())
                .status(req.getStatus() != null ? req.getStatus() : ServiceStatus.ACTIVE)
                .code(req.getCode())
                .description(req.getDescription())
                .isDeleted(false)      // <--- explicitly set
                .build();

        e = repo.save(e);
        return map(e);
    }

    @Override
    public DoctorServiceResponse get(Long doctorId, Long serviceId) {
        DoctorServiceItem e = repo.findById(serviceId)
                .filter(x -> x.getDoctor().getId().equals(doctorId))
                .orElseThrow(() -> new EntityNotFoundException("Service not found"));
        return map(e);
    }

    @Override
    public DoctorServiceResponse update(Long doctorId, Long serviceId, DoctorServiceUpdateRequest req) {
        DoctorServiceItem e = repo.findById(serviceId)
                .filter(x -> x.getDoctor().getId().equals(doctorId))
                .orElseThrow(() -> new EntityNotFoundException("Service not found"));

        if (!e.getName().equalsIgnoreCase(req.getName()) &&
                repo.existsByDoctor_IdAndNameIgnoreCase(doctorId, req.getName())) {
            throw new DataIntegrityViolationException("Duplicate service name for this doctor");
        }

        e.setName(req.getName().trim());
        e.setPrice(req.getPrice());
        if (req.getStatus() != null) e.setStatus(req.getStatus());
        e.setCode(req.getCode());
        e.setDescription(req.getDescription());
        e.setIsDeleted(req.getIsDeleted());      // <--- explicitly set

        e = repo.save(e);
        return map(e);
    }

    @Override
    public void delete(Long doctorId, Long serviceId) {
        DoctorServiceItem e = repo.findById(serviceId)
                .filter(x -> x.getDoctor().getId().equals(doctorId))
                .orElseThrow(() -> new EntityNotFoundException("Service not found"));
        repo.delete(e);
    }

    @Override
    public int bulkPatch(Long doctorId, BulkPatchRequest req) {
        List<DoctorServiceItem> items = repo.findAllById(req.getIds())
                .stream()
                .filter(x -> x.getDoctor().getId().equals(doctorId))
                .toList();

        switch (req.getOp()) {
            case ACTIVATE -> items.forEach(i -> i.setStatus(ServiceStatus.ACTIVE));
            case INACTIVATE -> items.forEach(i -> i.setStatus(ServiceStatus.INACTIVE));
            case PERCENT -> {
                if (req.getPercent() == null) throw new IllegalArgumentException("percent is required for op=PERCENT");
                items.forEach(i -> {
                    var factor = req.getPercent().divide(new BigDecimal("100"));
                    var delta = i.getPrice().multiply(factor);
                    i.setPrice(i.getPrice().add(delta).setScale(2, BigDecimal.ROUND_HALF_UP));
                });
            }
            case ABSOLUTE -> {
                if (req.getAbsolute() == null)
                    throw new IllegalArgumentException("absolute is required for op=ABSOLUTE");
                items.forEach(i -> i.setPrice(req.getAbsolute().setScale(2, BigDecimal.ROUND_HALF_UP)));
            }
        }

        repo.saveAll(items);
        return items.size();
    }
}
