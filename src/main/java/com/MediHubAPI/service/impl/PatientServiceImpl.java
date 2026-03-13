package com.MediHubAPI.service.impl;

import com.MediHubAPI.exception.ResourceNotFoundException;
import com.MediHubAPI.model.User;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final UserRepository userRepository;

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }
}
