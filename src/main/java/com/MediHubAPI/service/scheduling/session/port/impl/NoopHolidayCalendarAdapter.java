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
        if (fromInclusive == null || toInclusive == null) return List.of();

        // Mock master list (you can add more)
        List<HolidayDTO> all = List.of(
                new HolidayDTO(LocalDate.of(2026, 1, 26), "Republic Day"),
                new HolidayDTO(LocalDate.of(2026, 3, 25), "Holi (Sample)"),
                new HolidayDTO(LocalDate.of(2026, 8, 15), "Independence Day"),
                new HolidayDTO(LocalDate.of(2026, 10, 2), "Gandhi Jayanti"),
                new HolidayDTO(LocalDate.of(2026, 12, 25), "Christmas")
        );

        List<HolidayDTO> filtered = all.stream()
                .filter(h -> !h.dateISO().isBefore(fromInclusive) && !h.dateISO().isAfter(toInclusive))
                .toList();

        log.info("HolidayCalendarPort(MOCK): returning {} holiday(s) for range {}..{}",
                filtered.size(), fromInclusive, toInclusive);

        return filtered;
    }

}
