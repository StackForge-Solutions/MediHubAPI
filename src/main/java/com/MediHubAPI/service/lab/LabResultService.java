package com.MediHubAPI.service.lab;

import com.MediHubAPI.dto.lab.SaveLabResultRequest;
import com.MediHubAPI.model.billing.InvoiceItem;
import com.MediHubAPI.repository.InvoiceItemRepository;
import com.MediHubAPI.repository.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabResultService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    @Transactional
    public void saveResults(SaveLabResultRequest request, String authorizedBy) {
        var invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        for (SaveLabResultRequest.Item itemReq : request.getItems()) {
            InvoiceItem item = invoice.getItems().stream()
                    .filter(it -> it.getId().equals(itemReq.getInvoiceItemId()))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Invoice item not found: " + itemReq.getInvoiceItemId()));

            item.setSampleStatus(normalize(itemReq.getSampleStatus()));
            item.setResultValue(itemReq.getResult());
            item.setResultUnit(itemReq.getUnit());
            item.setReferenceRange(itemReq.getReference());
            item.setOutOfRange(itemReq.getOutOfRange());
            item.setAuthorized(itemReq.getAuthorized());
            if (Boolean.TRUE.equals(itemReq.getAuthorized())) {
                item.setAuthorizedBy(authorizedBy);
                item.setAuthorizedAt(LocalDateTime.now());
            }
        }

        invoiceItemRepository.saveAll(invoice.getItems());
        log.info("Lab results saved for invoiceId={}, items={}", request.getInvoiceId(), request.getItems().size());
    }

    private String normalize(String status) {
        if (status == null) return null;
        String s = status.trim().toUpperCase();
        if (s.isEmpty()) return null;
        if ("NOSHOW".equals(s)) return "NO_SHOW";
        return s;
    }
}
