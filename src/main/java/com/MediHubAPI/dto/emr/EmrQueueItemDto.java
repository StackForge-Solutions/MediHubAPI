package com.MediHubAPI.dto.emr;

public record EmrQueueItemDto(
        int rowId,
        String tokenNo,

        Long doctorId,
        Long appointmentId,

        String slotTime,   // "HH:mm:ss" (string for UI)
        String visitDate,  // "yyyy-MM-dd" (string for UI)

        Long patientId,
        String patientName,
        String ageSexLabel,
        String doctorName,
        String createdAtISO,
        String alerts,
        String status,
        boolean hasInsurance,
        boolean hasReferrer
) {}
