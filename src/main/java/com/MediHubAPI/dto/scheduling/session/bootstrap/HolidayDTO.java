package com.MediHubAPI.dto.scheduling.session.bootstrap;

import java.time.LocalDate;

public record HolidayDTO(
        LocalDate dateISO,
        String label
) {}
