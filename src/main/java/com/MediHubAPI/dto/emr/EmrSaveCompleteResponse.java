package com.MediHubAPI.dto.emr;

public record EmrSaveCompleteResponse(
        Long appointmentId,
        Long visitSummaryId,
        Long invoiceId,
        String invoiceQueue,
        String invoiceStatus,
        String appointmentStatus
) {
}
