 package com.MediHubAPI.service.scheduling.session.port.impl;

import com.MediHubAPI.dto.scheduling.session.bootstrap.DepartmentLiteDTO;
import com.MediHubAPI.repository.SpecializationRepository;
import com.MediHubAPI.service.scheduling.session.port.DepartmentDirectoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaDepartmentDirectoryAdapter implements DepartmentDirectoryPort {

    private final SpecializationRepository specializationRepository;

    @Override
    public List<DepartmentLiteDTO> listDepartments() {
        // We don't have a Department table. We reuse Specialization.department as "department label".
        List<String> depts = specializationRepository.findDistinctDepartments();

        // DepartmentLiteDTO is a DTO, not an entity. If your DTO has different fields, adjust here.
        // Using a generated id (stable for response only).
        AtomicLong seq = new AtomicLong(1);

        List<DepartmentLiteDTO> out = depts.stream()
                .map(name -> new DepartmentLiteDTO(seq.getAndIncrement(), name))
                .collect(Collectors.toList());

        log.info("DepartmentDirectoryPort.listDepartments: returned={}", out.size());
        return out;
    }
}
