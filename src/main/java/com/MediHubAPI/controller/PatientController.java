package com.MediHubAPI.controller;

import com.MediHubAPI.dto.PatientDetailsDto;
import com.MediHubAPI.dto.PatientResponseDto;
import com.MediHubAPI.model.User;
import com.MediHubAPI.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;


    @GetMapping("/by-date")
    public List<PatientDetailsDto> byDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return patientService.getPatientsByDate(date);
    }

    @GetMapping("/by-week")
    public List<PatientDetailsDto> byWeek(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return patientService.getPatientsByWeek(date);
    }

    @GetMapping("/by-month")
    public List<PatientDetailsDto> byMonth(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return patientService.getPatientsByMonth(date);
    }

    @GetMapping("/next")
    public List<PatientDetailsDto> next() {
        return patientService.getNextPatients();
    }

    @GetMapping("/previous")
    public List<PatientDetailsDto> previous() {
        return patientService.getPreviousPatients();
    }

}
