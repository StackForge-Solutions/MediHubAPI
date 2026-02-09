package com.MediHubAPI.service.patient;

import com.MediHubAPI.dto.patient.search.PatientSearchBy;
import com.MediHubAPI.dto.patient.search.PatientSearchResultDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.User;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.specification.PatientSearchSpecification;
import com.MediHubAPI.specification.UserSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientSearchService {

    private final UserRepository userRepository;

    @Transactional
    public List<PatientSearchResultDto> search(PatientSearchBy by, String query) {
        if (!StringUtils.hasText(query)) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "q is required");
        }
        String trimmed = query.trim();

        Specification<User> spec = Specification
                .where(UserSpecification.hasRole(ERole.PATIENT))
                .and(PatientSearchSpecification.matches(by, trimmed));

        return userRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PatientSearchResultDto toDto(User user) {
        return PatientSearchResultDto.builder()
                .id(user.getId())
                .hospitalId(user.getHospitalId())
                .fileNo(user.getFileNo())
                .fullName(buildFullName(user))
                .phone(user.getMobileNumber())
                .fatherName(user.getFathersName())
                .motherName(user.getMothersName())
                .dobISO(user.getDateOfBirth() == null ? null : user.getDateOfBirth().toString())
                .isInternational(Boolean.TRUE.equals(user.getInternational()))
                .build();
    }

    private String buildFullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(user.getTitle())) {
            sb.append(user.getTitle().trim()).append(' ');
        }
        if (StringUtils.hasText(user.getFirstName())) {
            sb.append(user.getFirstName().trim());
        }
        if (StringUtils.hasText(user.getLastName())) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(user.getLastName().trim());
        }
        return sb.toString().trim();
    }
}
