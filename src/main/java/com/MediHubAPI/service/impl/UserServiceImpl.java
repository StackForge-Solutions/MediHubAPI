package com.MediHubAPI.service.impl;

import com.MediHubAPI.config.RolePermissionMatrix;
import com.MediHubAPI.dto.UserCreateDto;
import com.MediHubAPI.dto.UserDto;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.exception.ResourceNotFoundException;
import com.MediHubAPI.exception.RolePermissionException;
import com.MediHubAPI.factory.UserFactory;
import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Role;
import com.MediHubAPI.model.User;
import com.MediHubAPI.repository.RoleRepository;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.UserService;
import com.MediHubAPI.specification.UserSpecification;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper mapper;
    private final UserFactory userFactory;
    @Autowired
    private RoleRepository roleRepository;


    public UserServiceImpl(UserRepository userRepository,
                           ModelMapper mapper,
                           UserFactory userFactory) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.userFactory = userFactory;
    }

    @Override
    public UserDto createUser(UserCreateDto userCreateDto) {
        validateUserCreateDto(userCreateDto);

        // Check for singleton SUPER_ADMIN
        if (userCreateDto.getRoles().contains(ERole.SUPER_ADMIN)) {
            boolean superAdminExists = userRepository.existsByRoles_Name(ERole.SUPER_ADMIN);
            if (superAdminExists) {
                throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Only one SUPER_ADMIN is allowed.");
            }

            // ✅ Allow unauthenticated user to register SUPER_ADMIN ONLY ONCE
            // Do not validate role creation permission here
        } else {
            // ✅ All other roles must be validated against logged-in user's roles
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Set<String> currentUserRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            for (ERole roleToAssign : userCreateDto.getRoles()) {
                validateRoleCreationPermission(currentUserRoles, roleToAssign);
            }
        }

        if (userRepository.existsByUsername(userCreateDto.getUsername())) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Username is already taken!");
        }

        if (userRepository.existsByEmail(userCreateDto.getEmail())) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Email is already in use!");
        }

        User user = userFactory.createUser(userCreateDto);
        User newUser = userRepository.save(user);

        return mapToDTO(newUser);
    }



    private void validateUserCreateDto(UserCreateDto userCreateDto) {
        if (userCreateDto.getUsername() == null || userCreateDto.getUsername().trim().isEmpty()) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }

        if (userCreateDto.getEmail() == null || userCreateDto.getEmail().trim().isEmpty()) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }

        if (userCreateDto.getPassword() == null || userCreateDto.getPassword().trim().isEmpty()) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }

        if (userCreateDto.getRoles() == null || userCreateDto.getRoles().isEmpty()) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "At least one role must be specified");
        }

    }

    private void validateRoleCreationPermission(Set<String> currentUserRoles, ERole newUserRole) {
        if (currentUserRoles.isEmpty()) {
            throw new RolePermissionException(HttpStatus.FORBIDDEN, "User role not assigned.");
        }

        for (String role : currentUserRoles) {
            try {
                ERole currentRole = ERole.valueOf(role.replace("ROLE_", ""));
                if (!RolePermissionMatrix.isRecognizedRole(currentRole)) {
                    throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "🏛️ Unrecognized " + currentRole + " Role in Backend");
                }

                if (RolePermissionMatrix.canCreate(currentRole, newUserRole)) {
                    return;
                }

            } catch (IllegalArgumentException e) {
                throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "🏛️ Unrecognized " + role + " Role in Backend");
            }
        }

        throw new RolePermissionException(HttpStatus.FORBIDDEN,
                "You don't have permission to create user with role " + newUserRole);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToDTO(user);
    }
    @Override
    public void updateUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
    }

    private UserDto mapToDTO(User user) {
        UserDto userDto = mapper.map(user, UserDto.class);
        Set<ERole> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        userDto.setRoles(roles);
        return userDto;
    }
    @Override
    public UserDto updateUserRolesByUsername(String username, Set<ERole> roles) {

        if (roles == null || roles.isEmpty()) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "At least one role must be specified");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Set<Role> roleEntities = roles.stream()
                .map(erole -> roleRepository.findByName(erole)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", erole.name())))
                .collect(Collectors.toSet());

        user.setRoles(roleEntities);
        User updatedUser = userRepository.save(user);

        return mapToDTO(updatedUser);
    }


    public User findUserOrThrow(Long id, String role) {
        return userRepository.findById(id)
                .orElseThrow(() -> new HospitalAPIException(
                        HttpStatus.NOT_FOUND,
                        role + " with ID " + id + " not found"
                ));
    }

    @Override
    public Page<UserDto> searchPatients(String keyword, String specialization, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        spec = spec.and(UserSpecification.hasRole(ERole.PATIENT));

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and(UserSpecification.searchByKeyword(keyword));
        }

        if (specialization != null && !specialization.isEmpty()) {
            spec = spec.and(UserSpecification.hasSpecialization(specialization));
        }

        return userRepository.findAll(spec, pageable)
                .map(this::mapToDTO);
    }


}
