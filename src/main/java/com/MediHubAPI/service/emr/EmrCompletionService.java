package com.MediHubAPI.service.emr;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.dto.emr.EmrSaveCompleteResponse;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.enums.AppointmentStatus;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;

@Service
@RequiredArgsConstructor
/**
 * Handles the consultation completion flow and routes the consultation
 * invoice to pharmacy.
 *
 * <p>Logic added here:
 * 1. Load and validate the appointment.
 * 2. Ensure the appointment is in a completable state.
 * 3. Ensure a visit summary already exists for the appointment.
 * 4. Lock the latest consultation invoice for the appointment.
 * 5. Move that invoice to the {@code PHARMACY} queue.
 * 6. Mark the appointment as {@code COMPLETED}.
 *
 * <p>Validation failures are raised as {@link ValidationException} so the
 * existing global exception handling returns a structured error payload.
 */
public class EmrCompletionService {

    private static final String PHARMACY_QUEUE = "PHARMACY";

    private final AppointmentRepository appointmentRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    /**
     * Completes the EMR workflow for an appointment after consultation review.
     *
     * <p>This method validates that:
     * - the appointmentId is a positive integer
     * - the appointment exists
     * - the appointment is not {@code CANCELLED} or {@code NO_SHOW}
     * - a {@link VisitSummary} exists
     * - a consultation invoice ({@code SERVICE} item type) exists
     * - the invoice is not {@code VOID}
     *
     * <p>Once validated, the latest consultation invoice is updated to queue
     * {@code PHARMACY} and the appointment status is changed to
     * {@code COMPLETED} in the same transaction.
     */
    public EmrSaveCompleteResponse completeConsultationAndRouteInvoiceToPharmacy(Long appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            throw new ValidationException(
                    "Validation failed",
                    List.of(new ValidationException.ValidationErrorDetail(
                            "appointmentId",
                            "appointmentId must be a positive integer"
                    ))
            );
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new HospitalAPIException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment not found"
                ));

        List<ValidationException.ValidationErrorDetail> details = new ArrayList<>();

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            details.add(new ValidationException.ValidationErrorDetail(
                    "appointmentStatus",
                    "cancelled appointments cannot be completed"
            ));
        }
        if (appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            details.add(new ValidationException.ValidationErrorDetail(
                    "appointmentStatus",
                    "no-show appointments cannot be completed"
            ));
        }

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElse(null);
        if (visitSummary == null) {
            details.add(new ValidationException.ValidationErrorDetail(
                    "appointmentId",
                    "visit summary must exist before consultation completion"
            ));
        }

        List<Invoice> invoices = invoiceRepository.findLatestByAppointmentIdAndItemTypeForUpdate(
                appointmentId,
                InvoiceDtos.ItemType.SERVICE,
                PageRequest.of(0, 1)
        );
        Invoice invoice = invoices.isEmpty() ? null : invoices.get(0);
        if (invoice == null) {
            details.add(new ValidationException.ValidationErrorDetail(
                    "invoice",
                    "consultation invoice not found for this appointment"
            ));
        } else if (invoice.getStatus() == Invoice.Status.VOID) {
            details.add(new ValidationException.ValidationErrorDetail(
                    "invoiceStatus",
                    "void invoice cannot be moved to pharmacy"
            ));
        }

        if (!details.isEmpty()) {
            throw new ValidationException("Validation failed", details);
        }

        invoice.setQueue(PHARMACY_QUEUE);
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
        }

        invoiceRepository.save(invoice);
        appointmentRepository.save(appointment);

        return new EmrSaveCompleteResponse(
                appointment.getId(),
                visitSummary.getId(),
                invoice.getId(),
                invoice.getQueue(),
                invoice.getStatus() == null ? null : invoice.getStatus().name(),
                appointment.getStatus() == null ? null : appointment.getStatus().name()
        );
    }
}
