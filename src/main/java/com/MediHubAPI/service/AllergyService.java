package com.MediHubAPI.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.MediHubAPI.dto.AllergyTemplateDto;
import com.MediHubAPI.dto.AppointmentAllergyRequest;
import com.MediHubAPI.dto.AppointmentAllergyResponse;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.model.Allergy;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.AllergyRepository;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllergyService {

    private static final List<AllergyTemplateDto> ALLERGY_TEMPLATES = List.of(
            new AllergyTemplateDto(101, "No Known Drug Allergy (NKDA)", "ALLERGIES", "English",
                    "No known drug allergy (NKDA)."),
            new AllergyTemplateDto(102, "No Known Allergies (NKA)", "ALLERGIES", "English",
                    "No known allergies (NKA)."),
            new AllergyTemplateDto(103, "Drug Allergy – Penicillin", "ALLERGIES", "English",
                    """
                            Penicillin / Amoxicillin – rash, itching.
                            Avoid beta-lactams if severe reaction history.
                            (If anaphylaxis history: avoid cephalosporins as per clinician judgement.)"""),
            new AllergyTemplateDto(104, "Drug Allergy – Sulfa (Sulfonamides)", "ALLERGIES", "English",
                    """
                            Sulfonamides (e.g., Cotrimoxazole) – rash / hives.
                            Avoid sulfa antibiotics.
                            (Non-antibiotic sulfonamides may be clinician-guided.)"""),
            new AllergyTemplateDto(105, "Drug Allergy – NSAIDs", "ALLERGIES", "English",
                    """
                            NSAIDs (Ibuprofen / Diclofenac) – wheeze / swelling / gastritis.
                            Avoid NSAIDs.
                            Use Paracetamol as alternative if suitable."""),
            new AllergyTemplateDto(106, "Food Allergy – Seafood", "ALLERGIES", "English",
                    "Seafood (fish / prawns) – hives, swelling.\n"
                            + "Avoid seafood and cross-contamination foods."),
            new AllergyTemplateDto(107, "Food Allergy – Nuts", "ALLERGIES", "English",
                    """
                            Peanuts / Tree nuts – hives, swelling.
                            Strict avoidance advised.
                            Carry emergency meds as per clinician advice."""),
            new AllergyTemplateDto(108, "Environmental – Dust / Pollen", "ALLERGIES", "English",
                    """
                            Dust – sneezing, nasal blockage.
                            Pollen – seasonal allergic rhinitis.
                            Triggers: dust exposure, seasonal change."""),
            new AllergyTemplateDto(109, "Contact Allergy – Latex / Adhesive", "ALLERGIES", "English",
                    """
                            Latex – itching / rash.
                            Adhesive tape – contact dermatitis.
                            Use latex-free gloves and hypoallergenic dressing."""),
            new AllergyTemplateDto(110, "Insect Sting Allergy", "ALLERGIES", "English",
                    "Bee/wasp sting – swelling, hives.\n"
                            + "If severe reaction history: avoid exposure and keep emergency plan."),
            new AllergyTemplateDto(111, "Drug Allergy – Contrast Dye (Radiology)", "ALLERGIES", "English",
                    """
                            Iodinated contrast – itching / rash after contrast study.
                            Inform radiology prior to CT contrast.
                            Premedication only as per clinician protocol."""),
            new AllergyTemplateDto(112, "Multiple Allergies (Common Set)", "ALLERGIES", "English",
                    """
                            Penicillin – rash.
                            NSAIDs – wheeze.
                            Dust – sneezing.
                            Seafood – hives.""")
    );

    private final VisitSummaryRepository visitSummaryRepository;
    private final AppointmentRepository appointmentRepository;
    private final AllergyRepository allergyRepository;

    @Transactional
    public AppointmentAllergyResponse saveOrUpdate(Long pathAppointmentId, AppointmentAllergyRequest request) {
        Long appointmentId = resolveAppointmentId(pathAppointmentId,
                request != null ? request.getAppointmentId() : null);

        if (request == null) {
            throw validation("body", "Request body is required");
        }

        boolean hasTemplate = request.getAllergyTemplateId() != null;
        boolean hasText = StringUtils.hasText(request.getAllergiesText());

        if (!hasTemplate && !hasText) {
            throw validation("allergyTemplateId", "Either allergyTemplateId or allergiesText is required");
        }
        if (hasTemplate && hasText) {
            throw validation("allergiesText", "Provide either allergyTemplateId or allergiesText, not both");
        }

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> createVisitSummary(appointmentId));

        Allergy allergy = allergyRepository.findByVisitSummary_Id(visitSummary.getId())
                .orElse(Allergy.builder().visitSummary(visitSummary).build());

        AllergyTemplateDto template = null;
        String resolvedText = request.getAllergiesText();

        if (hasTemplate) {
            template = findTemplate(request.getAllergyTemplateId())
                    .orElseThrow(() -> validation("allergyTemplateId",
                            "Invalid allergyTemplateId: " + request.getAllergyTemplateId()));
            resolvedText = template.getContentText();
        } else if (!StringUtils.hasText(resolvedText)) {
            throw validation("allergiesText", "allergiesText cannot be blank");
        }

        allergy.setAllergyTemplateId(template != null ? template.getId() : null);
        allergy.setAllergyTemplateName(template != null ? template.getName() : null);
        allergy.setCategory(template != null ? template.getCategory() : null);
        allergy.setLanguage(template != null ? template.getLanguage() : null);
        allergy.setAllergiesText(resolvedText);
        allergy.setVisitSummary(visitSummary);

        if (StringUtils.hasText(request.getVisitDate())) {
            visitSummary.setVisitDate(parseVisitDate(request.getVisitDate()));
        }

        Allergy saved = allergyRepository.save(allergy);
        visitSummary.setAllergy(saved);

        log.info("Saved allergies for appointmentId={}, templateId={}, visitSummaryId={}",
                appointmentId, saved.getAllergyTemplateId(), visitSummary.getId());

        return buildResponse(visitSummary, saved);
    }

    @Transactional(readOnly = true)
    public AppointmentAllergyResponse getByAppointmentId(Long appointmentId) {
        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> validation("appointmentId",
                        "VisitSummary not found for appointmentId " + appointmentId));

        Allergy allergy = allergyRepository.findByVisitSummary_Id(visitSummary.getId())
                .orElseThrow(() -> validation("appointmentId",
                        "No allergies recorded for appointmentId " + appointmentId));

        return buildResponse(visitSummary, allergy);
    }

    public List<AllergyTemplateDto> listTemplates() {
        return ALLERGY_TEMPLATES;
    }

    private Long resolveAppointmentId(Long pathAppointmentId, Long bodyAppointmentId) {
        if (pathAppointmentId == null && bodyAppointmentId == null) {
            throw validation("appointmentId", "appointmentId is required");
        }

        if ((pathAppointmentId != null && pathAppointmentId <= 0)
                || (bodyAppointmentId != null && bodyAppointmentId <= 0)) {
            throw validation("appointmentId", "appointmentId must be a positive number");
        }

        if (bodyAppointmentId != null && pathAppointmentId != null
                && !pathAppointmentId.equals(bodyAppointmentId)) {
            throw validation("appointmentId", "appointmentId in path and body must match");
        }

        return pathAppointmentId != null ? pathAppointmentId : bodyAppointmentId;
    }

    private VisitSummary createVisitSummary(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> validation("appointmentId", "Appointment not found with ID: " + appointmentId));

        VisitSummary visitSummary = VisitSummary.builder()
                .appointment(appointment)
                .doctor(appointment.getDoctor())
                .patient(appointment.getPatient())
                .visitDate(appointment.getAppointmentDate() != null
                        ? appointment.getAppointmentDate().toString()
                        : null)
                .visitTime(appointment.getSlotTime() != null
                        ? appointment.getSlotTime().toString()
                        : null)
                .build();

        VisitSummary saved = visitSummaryRepository.saveAndFlush(visitSummary);
        log.info("Created VisitSummary id={} for appointmentId={}", saved.getId(), appointmentId);
        return saved;
    }

    private AppointmentAllergyResponse buildResponse(VisitSummary visitSummary, Allergy allergy) {
        AllergyTemplateDto template = allergy.getAllergyTemplateId() != null
                ? findTemplate(allergy.getAllergyTemplateId()).orElse(null)
                : null;

        return AppointmentAllergyResponse.builder()
                .appointmentId(visitSummary.getAppointment() != null
                        ? visitSummary.getAppointment().getId() : null)
                .visitSummaryId(visitSummary.getId())
                .visitDate(visitSummary.getVisitDate())
                .allergyTemplateId(allergy.getAllergyTemplateId())
                .allergyTemplateName(allergy.getAllergyTemplateName() != null
                        ? allergy.getAllergyTemplateName()
                        : template != null ? template.getName() : null)
                .allergyCategory(allergy.getCategory() != null
                        ? allergy.getCategory()
                        : template != null ? template.getCategory() : null)
                .language(allergy.getLanguage() != null
                        ? allergy.getLanguage()
                        : template != null ? template.getLanguage() : null)
                .allergiesText(allergy.getAllergiesText())
                .build();
    }

    private Optional<AllergyTemplateDto> findTemplate(Integer id) {
        return ALLERGY_TEMPLATES.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    private String parseVisitDate(String raw) {
        try {
            return LocalDate.parse(raw).toString();
        } catch (DateTimeParseException ex) {
            throw validation("visitDate", "visitDate must be in ISO format yyyy-MM-dd");
        }
    }

    private ValidationException validation(String field, String message) {
        return new ValidationException(
                "Validation failed",
                List.of(new ValidationException.ValidationErrorDetail(field, message))
        );
    }
}
