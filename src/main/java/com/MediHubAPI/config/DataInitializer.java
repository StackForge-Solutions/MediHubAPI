package com.MediHubAPI.config;

import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.Role;
import com.MediHubAPI.model.Specialization;
import com.MediHubAPI.repository.RoleRepository;
import com.MediHubAPI.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final SpecializationRepository specializationRepository;

    @Override
    public void run(String... args) {
        // Initialize roles if they don't exist
        for (ERole role : ERole.values()) {
            if (!roleRepository.existsByName(role)) {
                roleRepository.save(new Role(role));
            }
        }

        addSpecializationIfNotExists("ORTHOPEDIC", "Orthopedics", "Bone and joint specialist");
        addSpecializationIfNotExists("DERMATOLOGIST", "Dermatology", "Skin specialist");
        addSpecializationIfNotExists("GYNECOLOGIST", "Gynecology", "Women’s health specialist");
        addSpecializationIfNotExists("GENERAL_PHYSICIAN", "General Medicine", "General health specialist");
        addSpecializationIfNotExists("PSYCHIATRIST", "Psychiatry", "Mental health specialist");
        addSpecializationIfNotExists("ENDOCRINOLOGIST", "Endocrinology", "Hormonal specialist");
        addSpecializationIfNotExists("PEDIATRICIAN", "Pediatrics", "Child specialist");
        addSpecializationIfNotExists("GENERAL_MEDICINE", "General Medicine", "Primary care / OPD");
        addSpecializationIfNotExists("PATIENT", "Patient", "End-user / patient account");
    }

    private void addSpecializationIfNotExists(String name, String department, String description) {
        boolean exists = specializationRepository.findAll()
                .stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(name));
        if (!exists) {
            Specialization spec = new Specialization();
            spec.setName(name);
            spec.setDepartment(department);
            spec.setDescription(description);
            specializationRepository.save(spec);
        }
    }
}
