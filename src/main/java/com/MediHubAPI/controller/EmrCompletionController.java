package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.emr.EmrSaveCompleteResponse;
import com.MediHubAPI.service.emr.EmrCompletionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emr")
@RequiredArgsConstructor
public class EmrCompletionController {

    private final EmrCompletionService emrCompletionService;

    @PostMapping("/appointments/{appointmentId}/complete-consultation")
    public ResponseEntity<ApiResponse<EmrSaveCompleteResponse>> completeConsultationAndRouteInvoiceToPharmacy(
            @PathVariable Long appointmentId,
            HttpServletRequest request
    ) {
        EmrSaveCompleteResponse data = emrCompletionService.completeConsultationAndRouteInvoiceToPharmacy(appointmentId);
        return ResponseEntity.ok(
                ApiResponse.success(data, request.getRequestURI(), "EMR saved and completed successfully")
        );
    }
}
