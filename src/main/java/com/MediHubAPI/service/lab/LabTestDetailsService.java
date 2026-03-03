package com.MediHubAPI.service.lab;

import com.MediHubAPI.dto.lab.LabTestDetailsDto;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoiceItem;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabTestDetailsService {

    private final com.MediHubAPI.repository.PatientLabInvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public LabTestDetailsDto getDetails(Long patientId) {

        // Pull latest invoice for patient containing LAB_TEST items (fallback to any invoice if none tagged)
        Invoice inv = invoiceRepository.findTopByPatient_IdAndItems_ItemTypeOrderByCreatedAtDesc(
                        patientId, com.MediHubAPI.dto.InvoiceDtos.ItemType.LAB_TEST)
                .or(() -> invoiceRepository.findTopByPatient_IdOrderByCreatedAtDesc(patientId))
                .orElseThrow(() -> new EntityNotFoundException("No invoice found for patientId=" + patientId));

        List<InvoiceItem> labItems = inv.getItems() == null ? List.of() : inv.getItems().stream()
                .filter(it -> it.getItemType() == com.MediHubAPI.dto.InvoiceDtos.ItemType.LAB_TEST || it.getItemType() == null)
                .sorted(Comparator.comparing(InvoiceItem::getSlNo))
                .toList();

        // Deduplicate tests by code+name (keep insertion order) to avoid doubled rows in responses
        List<LabTestDetailsDto.TestItem> tests = labItems.stream()
                .map(it -> LabTestDetailsDto.TestItem.builder()
                        .code(it.getCode())
                        .name(it.getName())
                        .authorized(it.getAuthorized())
                        .sampleStatus(it.getSampleStatus())
                        .result(it.getResultValue())
                        .unit(it.getResultUnit())
                        .reference(it.getReferenceRange())
                        .outOfRange(it.getOutOfRange())
                        .build())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                t -> (t.getCode() == null ? "" : t.getCode()) + "|" + (t.getName() == null ? "" : t.getName()),
                                t -> t,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ),
                        m -> List.copyOf(m.values())
                ));

        String billDate = inv.getIssuedAt() == null ? null : inv.getIssuedAt().toLocalDate().toString();
        String billNo = inv.getBillNumber();

        String patientName = inv.getPatient() == null ? null : fullName(inv.getPatient().getFirstName(), inv.getPatient().getLastName());
        String ageSex = null;
        if (inv.getPatient() != null) {
            var u = inv.getPatient();
            String sex = u.getSex() == null ? "Other" : switch (u.getSex()) { case MALE -> "Male"; case FEMALE -> "Female"; default -> "Other"; };
            String age = u.getDateOfBirth() == null ? "NA" : String.valueOf(java.time.Period.between(u.getDateOfBirth(), java.time.LocalDate.now()).getYears()) + " Y";
            ageSex = sex + " | (" + age + ")";
        }

        return LabTestDetailsDto.builder()
                .patientId(patientId)
                .name(patientName)
                .hospitalId(inv.getPatient() != null ? inv.getPatient().getHospitalId() : null)
                .ageSex(ageSex)
                .dob(inv.getPatient() != null && inv.getPatient().getDateOfBirth() != null ? inv.getPatient().getDateOfBirth().toString() : null)
                .billDate(billDate)
                .billNo(billNo)
                .tests(tests)
                .build();
    }

    private String fullName(String f, String l) {
        String ff = f == null ? "" : f.trim();
        String ll = l == null ? "" : l.trim();
        String out = (ff + " " + ll).trim();
        return out.isEmpty() ? null : out;
    }
}
