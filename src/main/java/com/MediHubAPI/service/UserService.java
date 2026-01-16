package com.MediHubAPI.service;


import com.MediHubAPI.dto.UserCreateDto;
import com.MediHubAPI.dto.UserDto;
import com.MediHubAPI.model.ERole;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface UserService {
    UserDto createUser(UserCreateDto userCreateDto);

    List<UserDto> getAllUsers();

    UserDto getUserById(Long id);

    void deleteUser(Long id);

    void updateUserStatus(Long userId, boolean enabled);

    UserDto updateUserRolesByUsername(String username, Set<ERole> roles);
    public Page<UserDto> searchPatients(String keyword, String specialization, Pageable pageable) ;

    List<UserDto> createUsersBulk(@Valid List<UserCreateDto> users);
}
