package com.MediHubAPI.util;

import com.MediHubAPI.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Provides a single place to resolve hospital identifiers.
 * If a value is missing or blank, we fall back to a configurable default.
 */
@Component
public class HospitalIdResolver {

    private final String defaultHospitalId;

    public HospitalIdResolver(@Value("${app.default-hospital-id:DEFAULT-HOSPITAL-ID}") String defaultHospitalId) {
        this.defaultHospitalId = defaultHospitalId;
    }

    /**
     * Return the provided hospitalId if present, otherwise the configured default.
     */
    public String resolve(String hospitalId) {
        return StringUtils.hasText(hospitalId) ? hospitalId.trim() : defaultHospitalId;
    }

    /**
     * Convenience overload for resolving directly from a User.
     */
    public String resolve(User user) {
        if (user == null) {
            return defaultHospitalId;
        }
        return resolve(user.getHospitalId());
    }

    public String getDefaultHospitalId() {
        return defaultHospitalId;
    }
}
