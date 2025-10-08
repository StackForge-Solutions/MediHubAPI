package com.MediHubAPI.config;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.Patient;
import com.MediHubAPI.model.User;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // ✅ Enable private field mapping
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setAmbiguityIgnored(true);

        // ✅ PatientCreateDto → Patient (skip ID)
        modelMapper.createTypeMap(PatientCreateDto.class, Patient.class)
                .addMappings(mapper -> mapper.skip(Patient::setId));

        // ✅ User → DoctorProfileDto (skip ID)
        modelMapper.createTypeMap(User.class, DoctorProfileDto.class)
                .addMappings(mapper -> mapper.skip(DoctorProfileDto::setId));

        // ✅ Appointment → WalkInAppointmentDto (skip time)
        modelMapper.createTypeMap(Appointment.class, WalkInAppointmentDto.class)
                .addMappings(mapper -> mapper.skip(WalkInAppointmentDto::setTime));

        // ✅ Appointment → AppointmentResponseDto (custom doctor/patient name mapping)
        TypeMap<Appointment, AppointmentResponseDto> appointmentMap =
                modelMapper.createTypeMap(Appointment.class, AppointmentResponseDto.class);

        appointmentMap.addMappings(mapper -> {
            mapper.using((MappingContext<Appointment, String> ctx) -> {
                Appointment src = ctx.getSource();
                if (src != null && src.getDoctor() != null) {
                    User doctor = src.getDoctor();
                    return doctor.getFirstName() + " " + doctor.getLastName();
                }
                return "Unknown Doctor";
            }).map(src -> src, AppointmentResponseDto::setDoctorName);

            mapper.using((MappingContext<Appointment, String> ctx) -> {
                Appointment src = ctx.getSource();
                if (src != null && src.getPatient() != null) {
                    User patient = src.getPatient();
                    return patient.getFirstName() + " " + patient.getLastName();
                }
                return "Unknown Patient";
            }).map(src -> src, AppointmentResponseDto::setPatientName);
        });


        //  Universal configuration for all mappings
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setAmbiguityIgnored(true);

        return modelMapper;
    }
}
