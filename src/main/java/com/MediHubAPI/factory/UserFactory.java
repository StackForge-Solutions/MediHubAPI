package com.MediHubAPI.factory;

import com.MediHubAPI.dto.UserCreateDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Role;
import com.MediHubAPI.model.Specialization;
import com.MediHubAPI.model.User;
import com.MediHubAPI.repository.RoleRepository;
import com.MediHubAPI.repository.SpecializationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UserFactory {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SpecializationRepository specializationRepository;

    public UserFactory(RoleRepository roleRepository, PasswordEncoder passwordEncoder, SpecializationRepository specializationRepository) {
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.specializationRepository = specializationRepository;
    }

    public User createUser(UserCreateDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // if applicable
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setMobileNumber(dto.getMobileNumber());

        // Fetch roles from DB
        Set<Role> roles = new HashSet<>();
        for (ERole roleEnum : dto.getRoles()) {
            Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "Role not found: " + roleEnum));
            roles.add(role);
        }
        user.setRoles(roles);

        // Check if user has DOCTOR role
        boolean isDoctor = roles.stream()
                .anyMatch(r -> r.getName() == ERole.DOCTOR);

        if (isDoctor) {
            if (dto.getSpecializationId() == null) {
                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Specialization is required for doctors");
            }
            Specialization specialization = specializationRepository.findById(dto.getSpecializationId())
                    .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "Specialization not found: " + dto.getSpecializationId()));
            user.setSpecialization(specialization);
        } else {
            if (dto.getSpecializationId() != null) {
                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Only doctors can have specialization");
            }
            user.setSpecialization(null);
        }

        return user;
    }


}
