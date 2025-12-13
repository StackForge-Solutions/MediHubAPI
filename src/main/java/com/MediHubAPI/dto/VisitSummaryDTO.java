package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisitSummaryDTO {

    /** Primary Key */
    private Long id;

    /** Doctor Info */
    private Long doctorId;
    private String doctorName;

    /** Patient Info */
    private Long patientId;
    private String patientName;

    /** Appointment Info */
    private Long appointmentId;
    private String appointmentDate;
    private String appointmentTime;

    /** Visit Info */
    private String visitDate;
    private String visitTime;

    /** Sub-sections */
    private List<ChiefComplaintDTO> chiefComplaints;
    private VitalsDTO vitals;
    private MedicalHistoryDTO medicalHistory;

    /** For UI state */
    private boolean existing;  // true = fetched existing record, false = new auto-created record

}
