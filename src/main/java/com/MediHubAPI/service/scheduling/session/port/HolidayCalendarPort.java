package com.MediHubAPI.service.scheduling.session.port;


import com.MediHubAPI.dto.scheduling.session.bootstrap.HolidayDTO;

import java.time.LocalDate;
import java.util.List;


public interface HolidayCalendarPort {
    List<HolidayDTO> listHolidays(LocalDate fromInclusive, LocalDate toInclusive);
}