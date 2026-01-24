package com.MediHubAPI.service.scheduling.session.port.impl;

import com.MediHubAPI.dto.scheduling.session.bootstrap.HolidayDTO;
import com.MediHubAPI.service.scheduling.session.port.HolidayCalendarPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class NoopHolidayCalendarAdapter implements HolidayCalendarPort {

    @Override
    public List<HolidayDTO> listHolidays(LocalDate fromInclusive, LocalDate toInclusive) {
        log.info("HolidayCalendarPort(NOOP): returning 0 holidays for range {}..{}", fromInclusive, toInclusive);
        return List.of();
    }
}
