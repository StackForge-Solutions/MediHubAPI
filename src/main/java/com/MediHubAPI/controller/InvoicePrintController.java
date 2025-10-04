package com.MediHubAPI.controller;

import com.MediHubAPI.service.InvoicePrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoicePrintController {

    private final InvoicePrintService printService;

    @GetMapping("/{id}/print")
    public ResponseEntity<?> print(@PathVariable Long id,
                                   @RequestParam(defaultValue = "pdf") String format) {

        var vm = printService.build(id);
        String html = printService.renderHtml(vm);

        if ("html".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        }

        byte[] pdf = printService.renderPdf(html);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
