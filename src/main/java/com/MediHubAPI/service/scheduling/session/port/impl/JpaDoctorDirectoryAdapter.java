package com.MediHubAPI.service.scheduling.session.port.impl;

import com.MediHubAPI.dto.scheduling.session.bootstrap.DoctorLiteDTO;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.User;
 import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.scheduling.session.port.DoctorDirectoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaDoctorDirectoryAdapter implements DoctorDirectoryPort {

    private final UserRepository userRepository;

    @Override
    public List<DoctorLiteDTO> listDoctors() {
        // Efficient path if you added repository query:
        List<User> users = userRepository.findByRoles_Name(ERole.DOCTOR);

        // Defensive filter (kept in case repo query is changed later)
        List<DoctorLiteDTO> doctors = users.stream()
                .filter(u -> u.getRoles() != null && u.getRoles().stream()
                        .anyMatch(r -> r != null && r.getName() == ERole.DOCTOR))
                .map(this::toDoctorLite)
                .collect(Collectors.toList());

        log.info("DoctorDirectoryPort.listDoctors: returned={}", doctors.size());
        return doctors;
    }

    private DoctorLiteDTO toDoctorLite(User u) {
        String fullName = safe(u.getFirstName()) + (isBlank(u.getLastName()) ? "" : (" " + u.getLastName()));
        String specializationName = null;

        // TODO: If your User has specialization mapped differently, replace this safely.
        if (u.getSpecialization() != null) {
            specializationName = u.getSpecialization().getName();
        }

        // Record construction (NO builder)
        return new DoctorLiteDTO(
                u.getId(),
                fullName.trim(),
                specializationName,
                u.isEnabled()
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
