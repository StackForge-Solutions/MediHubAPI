package com.MediHubAPI.service;


import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.MediHubAPI.dto.UserCreateDto;
import com.MediHubAPI.dto.UserDto;
import com.MediHubAPI.dto.UserUpdateDto;
import com.MediHubAPI.model.ERole;

public interface UserService {

    UserDto createUser(UserCreateDto userCreateDto);

    Page<UserDto> getAllUsers(Pageable pageable);

    UserDto getUserById(Long id);

    void deleteUser(Long id);

    UserDto updateUser(Long userId, UserUpdateDto userUpdateDto);

    UserDto updateUserRolesByUsername(String username, Set<ERole> roles);

    Page<UserDto> searchPatients(String keyword, String specialization, Pageable pageable);
}
