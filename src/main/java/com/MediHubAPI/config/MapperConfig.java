package com.MediHubAPI.config;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.ChiefComplaint;
import com.MediHubAPI.model.Patient;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.VisitSummary;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // ----------------- Global Configuration -----------------
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setAmbiguityIgnored(true);

        // ----------------- Patient Mapping -----------------
        modelMapper.createTypeMap(PatientCreateDto.class, Patient.class)
                .addMappings(mapper -> mapper.skip(Patient::setId));

        // ----------------- User → DoctorProfileDto -----------------
        modelMapper.createTypeMap(User.class, DoctorProfileDto.class)
                .addMappings(mapper -> mapper.skip(DoctorProfileDto::setId));

        // ----------------- Appointment Mappings -----------------
        modelMapper.createTypeMap(Appointment.class, WalkInAppointmentDto.class)
                .addMappings(mapper -> mapper.skip(WalkInAppointmentDto::setTime));

        TypeMap<Appointment, AppointmentResponseDto> appointmentMap =
                modelMapper.createTypeMap(Appointment.class, AppointmentResponseDto.class);

        appointmentMap.addMappings(mapper -> {
            // Doctor Name
            mapper.using((MappingContext<Appointment, String> ctx) -> {
                Appointment src = ctx.getSource();
                if (src != null && src.getDoctor() != null) {
                    User doc = src.getDoctor();
                    String first = doc.getFirstName() != null ? doc.getFirstName() : "";
                    String last = doc.getLastName() != null ? doc.getLastName() : "";
                    return (first + " " + last).trim();
                }
                return "Unknown Doctor";
            }).map(src -> src, AppointmentResponseDto::setDoctorName);

            // Patient Name
            mapper.using((MappingContext<Appointment, String> ctx) -> {
                Appointment src = ctx.getSource();
                if (src != null && src.getPatient() != null) {
                    User p = src.getPatient();
                    String first = p.getFirstName() != null ? p.getFirstName() : "";
                    String last = p.getLastName() != null ? p.getLastName() : "";
                    return (first + " " + last).trim();
                }
                return "Unknown Patient";
            }).map(src -> src, AppointmentResponseDto::setPatientName);
        });

        // ----------------- ChiefComplaint → ChiefComplaintDTO -----------------
        modelMapper.createTypeMap(ChiefComplaint.class, ChiefComplaintDTO.class);

        // ----------------- VisitSummary → VisitSummaryDTO -----------------
        TypeMap<VisitSummary, VisitSummaryDTO> visitSummaryMap =
                modelMapper.createTypeMap(VisitSummary.class, VisitSummaryDTO.class);

        visitSummaryMap.addMappings(mapper -> {
            // Doctor ID
            mapper.map(src -> src.getDoctor() != null ? src.getDoctor().getId() : null,
                    VisitSummaryDTO::setDoctorId);

            // Doctor Name
            mapper.using((MappingContext<VisitSummary, String> ctx) -> {
                VisitSummary src = ctx.getSource();
                if (src.getDoctor() != null) {
                    User doc = src.getDoctor();
                    String first = doc.getFirstName() != null ? doc.getFirstName() : "";
                    String last = doc.getLastName() != null ? doc.getLastName() : "";
                    return (first + " " + last).trim();
                }
                return "Unknown Doctor";
            }).map(src -> src, VisitSummaryDTO::setDoctorName);

            // Patient ID
            mapper.map(src -> src.getPatient() != null ? src.getPatient().getId() : null,
                    VisitSummaryDTO::setPatientId);

            // Patient Name
            mapper.using((MappingContext<VisitSummary, String> ctx) -> {
                VisitSummary src = ctx.getSource();
                if (src.getPatient() != null) {
                    User p = src.getPatient();
                    String first = p.getFirstName() != null ? p.getFirstName() : "";
                    String last = p.getLastName() != null ? p.getLastName() : "";
                    return (first + " " + last).trim();
                }
                return "Unknown Patient";
            }).map(src -> src, VisitSummaryDTO::setPatientName);

            // Chief complaints
            mapper.using((MappingContext<VisitSummary, java.util.List<ChiefComplaintDTO>> ctx) -> {
                VisitSummary src = ctx.getSource();
                if (src.getChiefComplaints() == null) return java.util.List.of();
                return src.getChiefComplaints().stream()
                        .map(c -> new ChiefComplaintDTO(
                                c.getComplaint(),
                                c.getYears(),
                                c.getMonths(),
                                c.getWeeks(),
                                c.getDays(),
                                c.getSinceYear(),
                                c.getBodyPart()
                        ))
                        .collect(Collectors.toList());
            }).map(src -> src, VisitSummaryDTO::setChiefComplaints);
        });

        return modelMapper;
    }
}
