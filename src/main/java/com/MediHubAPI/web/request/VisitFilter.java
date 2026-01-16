package com.MediHubAPI.web.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class VisitFilter {
    // period = DAY | WEEK | MONTH | RANGE (default DAY)
    private String period = "DAY";

    // base date used for DAY/WEEK/MONTH windows; default today if null
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    // for RANGE only
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    // optional narrowing
    private Long doctorId;
    private Long patientId;

    // pagination
    private Integer page = 0;
    private Integer size = 20;

    // optional sort, e.g. "appointmentDate,desc;slotTime,desc"
    private String sort;

    // NEW (optional)
    private Boolean hasLabTests; // null or false = ignore
}
