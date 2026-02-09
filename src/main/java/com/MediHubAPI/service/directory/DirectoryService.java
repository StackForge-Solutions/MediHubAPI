package com.MediHubAPI.service.directory;

import com.MediHubAPI.dto.directory.DepartmentDto;
import com.MediHubAPI.dto.directory.DoctorDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.User;
import com.MediHubAPI.repository.SpecializationRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.specification.UserSpecification;
import jakarta.persistence.criteria.JoinType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final SpecializationRepository specializationRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<DepartmentDto> listDepartments() {
        return buildDepartmentIndex().departments();
    }

    @Transactional
    public List<DoctorDto> listDoctors(Long departmentId) {
        DepartmentIndex index = buildDepartmentIndex();
        String departmentName = null;
        if (departmentId != null) {
            if (departmentId <= 0) {
                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "departmentId must be positive");
            }
            departmentName = index.getNameById(departmentId);
            if (departmentName == null) {
                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "departmentId is invalid");
            }
        }

        Specification<User> spec = Specification.where(UserSpecification.hasRole(ERole.DOCTOR));
        if (departmentName != null) {
            String lowerDept = departmentName.toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> {
                var join = root.join("specialization", JoinType.LEFT);
                return cb.equal(cb.lower(join.get("department")), lowerDept);
            });
        }

        List<User> doctors = userRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));

        return doctors.stream()
                .map(user -> toDoctorDto(user, index))
                .collect(Collectors.toList());
    }

    private DoctorDto toDoctorDto(User user, DepartmentIndex index) {
        String name = buildFullName(user);
        String speciality = user.getSpecialization() != null ? user.getSpecialization().getName() : null;
        String deptName = user.getSpecialization() == null ? null : user.getSpecialization().getDepartment();
        Long deptId = deptName == null ? null : index.getIdByName(deptName);
        return DoctorDto.builder()
                .id(user.getId())
                .name(name)
                .speciality(speciality)
                .departmentId(deptId)
                .build();
    }

    private String buildFullName(User user) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(user.getTitle())) parts.add(user.getTitle().trim());
        if (StringUtils.hasText(user.getFirstName())) parts.add(user.getFirstName().trim());
        if (StringUtils.hasText(user.getLastName())) parts.add(user.getLastName().trim());
        return String.join(" ", parts).trim();
    }

    private DepartmentIndex buildDepartmentIndex() {
        List<String> names = specializationRepository.findDistinctDepartments();
        AtomicLong seq = new AtomicLong(1);
        List<DepartmentDto> departments = names.stream()
                .map(name -> new DepartmentDto(seq.getAndIncrement(), name))
                .collect(Collectors.toList());
        return new DepartmentIndex(departments);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private record DepartmentIndex(List<DepartmentDto> departments) {
        private String getNameById(Long id) {
            if (id == null) return null;
            return departments.stream()
                    .filter(d -> d.getId().equals(id))
                    .map(DepartmentDto::getName)
                    .findFirst()
                    .orElse(null);
        }

        private Long getIdByName(String name) {
            if (name == null) return null;
            String normalized = DirectoryService.normalize(name);
            return departments.stream()
                    .filter(d -> DirectoryService.normalize(d.getName()).equals(normalized))
                    .map(DepartmentDto::getId)
                    .findFirst()
                    .orElse(null);
        }
    }
}
