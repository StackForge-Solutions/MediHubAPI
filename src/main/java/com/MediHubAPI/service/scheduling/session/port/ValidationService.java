package com.MediHubAPI.service.scheduling.session.port;

import com.MediHubAPI.dto.scheduling.session.validate.ValidateRequest;
import com.MediHubAPI.dto.scheduling.session.validate.ValidateResponse;

public interface ValidationService {
    ValidateResponse validate(ValidateRequest request);
}
