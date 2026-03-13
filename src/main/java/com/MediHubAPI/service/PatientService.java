package com.MediHubAPI.service;

import com.MediHubAPI.model.User;

public interface PatientService {

    User findById(Long id);
}
