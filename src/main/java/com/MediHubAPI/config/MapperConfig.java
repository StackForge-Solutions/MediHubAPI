package com.MediHubAPI.config;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.model.*;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.modelmapper.AbstractConverter;
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

        // ----------------- 🌐 Global Configuration -----------------
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setAmbiguityIgnored(true)
                .setSkipNullEnabled(true)
                .setPropertyCondition(ctx -> ctx.getSource() != null);

        // ----------------- 🧩 Global Converter to unwrap Hibernate proxies -----------------
        modelMapper.addConverter(new AbstractConverter<Object, Object>() {
            @Override
            protected Object convert(Object source) {
                if (source instanceof HibernateProxy) {
                    return Hibernate.unproxy(source);
                }
                return source;
            }
        });

        // ----------------- Patient Mapping -----------------
        modelMapper.createTypeMap(PatientCreateDto.class, Patient.class)
                .addMappings(mapper -> mapper.skip(Patient::setId));

        // ----------------- User → DoctorProfileDto -----------------
        modelMapper.createTypeMap(User.class, DoctorProfileDto.class)
                .addMappings(mapper -> mapper.skip(DoctorProfileDto::setId));

        // ----------------- Appointment → AppointmentResponseDto -----------------
        TypeMap<Appointment, AppointmentResponseDto> appointmentMap =
                modelMapper.createTypeMap(Appointment.class, AppointmentResponseDto.class);

        appointmentMap.addMappings(mapper -> {
            // Doctor Name
            mapper.using((MappingContext<Appointment, String> ctx) -> {
                Appointment src = ctx.getSource();
                if (src != null && src.getDoctor() != null) {
                    User doc = unwrapProxy(src.getDoctor());
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
                    User p = unwrapProxy(src.getPatient());
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
            mapper.map(src -> {
                User doctor = unwrapProxy(src.getDoctor());
                return doctor != null ? doctor.getId() : null;
            }, VisitSummaryDTO::setDoctorId);

            // Doctor Name
            mapper.using((MappingContext<VisitSummary, String> ctx) -> {
                VisitSummary src = ctx.getSource();
                if (src != null && src.getDoctor() != null) {
                    User doc = unwrapProxy(src.getDoctor());
                    String first = doc.getFirstName() != null ? doc.getFirstName() : "";
                    String last = doc.getLastName() != null ? doc.getLastName() : "";
                    return (first + " " + last).trim();
                }
                return "Unknown Doctor";
            }).map(src -> src, VisitSummaryDTO::setDoctorName);

            // Patient ID
            mapper.map(src -> {
                User patient = unwrapProxy(src.getPatient());
                return patient != null ? patient.getId() : null;
            }, VisitSummaryDTO::setPatientId);

            // Patient Name
            mapper.using((MappingContext<VisitSummary, String> ctx) -> {
                VisitSummary src = ctx.getSource();
                if (src != null && src.getPatient() != null) {
                    User p = unwrapProxy(src.getPatient());
                    String first = p.getFirstName() != null ? p.getFirstName() : "";
                    String last = p.getLastName() != null ? p.getLastName() : "";
                    return (first + " " + last).trim();
                }
                return "Unknown Patient";
            }).map(src -> src, VisitSummaryDTO::setPatientName);

            // Chief Complaints
            mapper.using((MappingContext<VisitSummary, java.util.List<ChiefComplaintDTO>> ctx) -> {
                VisitSummary src = ctx.getSource();
                if (src == null || src.getChiefComplaints() == null)
                    return java.util.List.of();

                return src.getChiefComplaints().stream()
                        .map(c -> new ChiefComplaintDTO(
                                c.getId(),
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

    // ----------------- 🔧 Utility: Hibernate Proxy Unwrapper -----------------
    private static <T> T unwrapProxy(T entity) {
        if (entity instanceof HibernateProxy) {
            return (T) Hibernate.unproxy(entity);
        }
        return entity;
    }
}
