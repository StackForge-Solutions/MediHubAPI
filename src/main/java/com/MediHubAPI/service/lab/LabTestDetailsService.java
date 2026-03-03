package com.MediHubAPI.service.lab;

import com.MediHubAPI.dto.lab.LabTestDetailsDto;
import com.MediHubAPI.dto.lab.UpdateLabTestDetailsRequest;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoiceItem;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import com.MediHubAPI.util.HospitalIdResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabTestDetailsService {

    private final com.MediHubAPI.repository.PatientLabInvoiceRepository invoiceRepository;
    private final HospitalIdResolver hospitalIdResolver;

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
                        .code(resolveTestCode(it))
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
            String age = u.getDateOfBirth() == null ? "NA" :
                java.time.Period.between(u.getDateOfBirth(), java.time.LocalDate.now()).getYears() + " Y";
            ageSex = sex + " | (" + age + ")";
        }

        return LabTestDetailsDto.builder()
                .patientId(patientId)
                .name(patientName)
                .hospitalId(hospitalIdResolver.resolve(inv.getPatient() != null ? inv.getPatient().getHospitalId() : null))
                .ageSex(ageSex)
                .dob(inv.getPatient() != null && inv.getPatient().getDateOfBirth() != null ? inv.getPatient().getDateOfBirth().toString() : null)
                .billDate(billDate)
                .billNo(billNo)
                .tests(tests)
                .build();
    }

    @Transactional
    public void updateLabTestDetails(UpdateLabTestDetailsRequest request, String authorizedBy) {
        Invoice invoice = invoiceRepository.findByPatient_IdAndBillNumber(request.getPatientId(), request.getBillNo())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No invoice found for patientId=" + request.getPatientId() + ", billNo=" + request.getBillNo()
                ));

        if (request.getRoom() != null && !request.getRoom().isBlank()) {
            invoice.setRoom(request.getRoom().trim());
        }

        LocalDate effectiveDate = parseDate(request.getBillDate());
        if (effectiveDate == null) {
            effectiveDate = parseDate(request.getDate());
        }
        if (effectiveDate != null) {
            LocalDateTime currentIssuedAt = invoice.getIssuedAt();
            invoice.setIssuedAt(currentIssuedAt == null
                    ? effectiveDate.atStartOfDay()
                    : effectiveDate.atTime(currentIssuedAt.toLocalTime()));
        }

        List<InvoiceItem> labItems = invoice.getItems() == null ? List.of() : invoice.getItems().stream()
                .filter(it -> it.getItemType() == com.MediHubAPI.dto.InvoiceDtos.ItemType.LAB_TEST || it.getItemType() == null)
                .toList();

        for (UpdateLabTestDetailsRequest.TestItem test : request.getTests()) {
            InvoiceItem item = labItems.stream()
                    .filter(it -> matchesTest(it, test.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Lab test not found for code=" + test.getCode()));

            item.setSampleStatus(normalizeStatus(test.getSampleStatus()));
            item.setResultValue(test.getResult());
            item.setResultUnit(test.getUnit());
            item.setReferenceRange(test.getReference());
            item.setOutOfRange(parseOutOfRange(test.getOutOfRange()));
            item.setAuthorized(test.getAuthorized());

            if (Boolean.TRUE.equals(test.getAuthorized())) {
                item.setAuthorizedBy(authorizedBy);
                item.setAuthorizedAt(LocalDateTime.now());
            } else if (Boolean.FALSE.equals(test.getAuthorized())) {
                item.setAuthorizedBy(null);
                item.setAuthorizedAt(null);
            }
        }

        invoiceRepository.save(invoice);
    }

    private String fullName(String f, String l) {
        String ff = f == null ? "" : f.trim();
        String ll = l == null ? "" : l.trim();
        String out = (ff + " " + ll).trim();
        return out.isEmpty() ? null : out;
    }

    private String resolveTestCode(InvoiceItem item) {
        if (item.getCode() != null && !item.getCode().isBlank()) {
            return item.getCode();
        }
        if (item.getRefId() != null) {
            return "LAB_PTEST_" + item.getRefId();
        }
        return item.getId() == null ? null : "LAB_ITEM_" + item.getId();
    }

    private boolean matchesTest(InvoiceItem item, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.equals(resolveTestCode(item))) {
            return true;
        }
        if (item.getCode() != null && normalizedCode.equals(item.getCode().trim())) {
            return true;
        }
        if (normalizedCode.startsWith("LAB_ITEM_") && item.getId() != null) {
            return normalizedCode.equals("LAB_ITEM_" + item.getId());
        }
        if (normalizedCode.startsWith("LAB_PTEST_") && item.getRefId() != null) {
            return normalizedCode.equals("LAB_PTEST_" + item.getRefId());
        }
        return false;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        if ("NOSHOW".equals(normalized)) {
            return "NO_SHOW";
        }
        return normalized;
    }

    private Boolean parseOutOfRange(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        if ("YES".equalsIgnoreCase(text) || "TRUE".equalsIgnoreCase(text) || "Y".equalsIgnoreCase(text)) {
            return true;
        }
        if ("NO".equalsIgnoreCase(text) || "FALSE".equalsIgnoreCase(text) || "N".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }
}
