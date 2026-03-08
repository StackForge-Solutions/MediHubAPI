package com.MediHubAPI.service.pharmacy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.MediHubAPI.dto.pharmacy.PharmacyQueueItemDto;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.repository.projection.PharmacyQueueRowProjection;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyQueueService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String PHARMACY_QUEUE_SQL = """
            select concat('TKN-', lpad(i.token_no, 3, '0')) as tokenNo, pu.hospital_id as patientId,
                   concat(pu.first_name, ' ', pu.last_name) as patientName, concat(du.first_name, ' ', du.last_name) as doctorName,
                   i.created_at as createdAt,
                   (upper(coalesce(i.queue, '')) like '%INSUR%') as hasInsurance,
                   (pat.referrer_name is not null or pat.referrer_number is not null) as hasReferrer,
                   case i.status when 'DRAFT' then 'Waiting' when 'ISSUED' then 'Waiting' when 'PAID' then 'Completed' else i.status end as status
            from invoices i
            join users pu on pu.id = i.patient_id
            join users du on du.id = i.doctor_id
            left join patients pat on pat.user_id = i.patient_id
            where i.created_at >= :start and i.created_at < :end
              and (i.queue is null or upper(i.queue) like '%PHARM%')
            order by i.created_at asc
            """;

    private final InvoiceRepository invoiceRepository;

    public List<PharmacyQueueItemDto> getQueue(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        log.info("Pharmacy queue fetch window start={} end={}", start, end);
        log.debug("Pharmacy queue SQL (MySQL): {}\n-- params: start='{}', end='{}'", PHARMACY_QUEUE_SQL, start, end);
        List<PharmacyQueueRowProjection> rows = invoiceRepository.fetchPharmacyQueue(start, end);
        if (rows == null) {
            rows = List.of();
        }
        log.info("Pharmacy queue rows fetched={}", rows.size());
        List<PharmacyQueueItemDto> out = new ArrayList<>(rows.size());
        int rowId = 1;
        for (PharmacyQueueRowProjection r : rows) {
            out.add(PharmacyQueueItemDto.builder()
                    .rowId(rowId++)
                    .tokenNo(r.getTokenNo())
                    .patientId(r.getPatientId())
                    .patientName(r.getPatientName())
                    .doctorName(r.getDoctorName())
                    .createdAt(
                            r.getCreatedAt() != null ? r.getCreatedAt().atOffset(java.time.ZoneOffset.UTC).format(ISO) :
                                    null)
                    .hasInsurance(toBool(r.getHasInsurance()))
                    .hasReferrer(toBool(r.getHasReferrer()))
                    .status(r.getStatus())
                    .build());
        }
        return out;
    }

    private boolean toBool(Integer val) {
        return val != null && val != 0;
    }
}
