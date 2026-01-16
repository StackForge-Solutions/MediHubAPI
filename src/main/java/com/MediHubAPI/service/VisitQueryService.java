package com.MediHubAPI.service;

import com.MediHubAPI.dto.VisitRowDTO;
import com.MediHubAPI.repository.AppointmentQueryRepository;
import com.MediHubAPI.web.request.VisitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitQueryService {

    private final AppointmentQueryRepository repo;

    public Page<VisitRowDTO> search(VisitFilter f) {
        LocalDate today = LocalDate.now();
        String period = (f.getPeriod() == null) ? "DAY" : f.getPeriod().trim().toUpperCase();

        LocalDate base = (f.getDate() != null) ? f.getDate() : today;
        LocalDate start;
        LocalDate end;

        switch (period) {
            case "WEEK" -> {
                DayOfWeek first = DayOfWeek.MONDAY;
                start = base.with(first);
                end   = start.plusDays(6);
            }
            case "MONTH" -> {
                start = base.withDayOfMonth(1);
                end   = start.plusMonths(1).minusDays(1);
            }
            case "RANGE" -> {
                start = (f.getFrom() != null) ? f.getFrom() : today;
                end   = (f.getTo()   != null) ? f.getTo()   : today;
            }
            case "DAY" -> {
                start = base;
                end   = base;
            }
            default -> {
                start = base;
                end   = base;
            }
        }


        Pageable pageable = buildPageable(f);
        return repo.findVisitRows(start, end, f.getDoctorId(), f.getPatientId(),  f.getHasLabTests(), pageable);

    }

    private Pageable buildPageable(VisitFilter f) {
        int page = (f.getPage() == null || f.getPage() < 0) ? 0 : f.getPage();
        int size = (f.getSize() == null || f.getSize() <= 0) ? 20 : f.getSize();

        Sort sort = Sort.by(Sort.Direction.DESC, "appointmentDate")
                .and(Sort.by(Sort.Direction.DESC, "slotTime"))
                .and(Sort.by(Sort.Direction.DESC, "id")); // stable pagination

        if (f.getSort() != null && !f.getSort().isBlank()) {
            List<Sort.Order> orders = new ArrayList<>();
            for (String token : f.getSort().split(";")) {
                String[] parts = token.split(",");
                String prop = parts[0].trim();
                Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                        ? Sort.Direction.ASC : Sort.Direction.DESC;
                // Only allow properties that map to query ORDER BY columns safely
                if (prop.equals("appointmentDate") || prop.equals("slotTime") || prop.equals("id")) {
                    orders.add(new Sort.Order(dir, prop));
                }
            }
            if (!orders.isEmpty()) sort = Sort.by(orders);
        }

        return PageRequest.of(page, size, sort);
    }
}
