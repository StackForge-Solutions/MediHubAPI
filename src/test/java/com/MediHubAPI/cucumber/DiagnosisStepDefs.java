package com.MediHubAPI.cucumber;

import com.MediHubAPI.dto.diagnosis.CreateDiagnosisRequest;
import com.MediHubAPI.dto.diagnosis.DiagnosisRowResponse;
import com.MediHubAPI.exception.diagnosis.DiagnosisValidationException;
import com.MediHubAPI.service.DiagnosisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScenarioScope
public class DiagnosisStepDefs {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DiagnosisService diagnosisService;

    private ResultActions latestResponse;

    @Given("a diagnosis create response for appointment {long}")
    public void diagnosisCreateSuccess(Long appointmentId) {
        DiagnosisRowResponse row = new DiagnosisRowResponse(
                "Influenza",
                "0 day",
                Instant.parse("2026-01-01T00:00:00Z"),
                false,
                true,
                null
        );
        when(diagnosisService.createDiagnosis(eq(appointmentId), any(CreateDiagnosisRequest.class))).thenReturn(row);
    }

    @Given("diagnosis create will fail for appointment {long} with sinceYear in future")
    public void diagnosisCreateFutureYear(Long appointmentId) {
        DiagnosisValidationException ex = new DiagnosisValidationException(
                "DIAGNOSIS_SINCE_YEAR_FUTURE",
                "VALIDATION_ERROR",
                "sinceYear cannot be in the future",
                Map.of("sinceYear", "sinceYear cannot be in the future"),
                List.of("sinceYear cannot be in the future")
        );
        when(diagnosisService.createDiagnosis(eq(appointmentId), any(CreateDiagnosisRequest.class))).thenThrow(ex);
    }

    @When("I POST {string} with appointmentId {long} and diagnosis payload")
    public void iPostWithAppointmentAndPayload(String path, Long appointmentId) throws Exception {
        String body = """
                {
                  "source": "doctor",
                  "name": "Influenza",
                  "years": 0,
                  "months": 0,
                  "days": 3,
                  "sinceYear": 2026,
                  "chronic": false,
                  "primary": true,
                  "comments": "High fever",
                  "currentName": "Influenza"
                }
                """;
        latestResponse = mockMvc.perform(
                MockMvcRequestBuilders.post(path)
                        .param("appointmentId", appointmentId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON)
        );
    }

    @Then("the response status is {int}")
    public void responseStatus(int status) throws Exception {
        latestResponse.andExpect(status().is(status));
    }

    @And("the response contains diagnosis name {string}")
    public void responseContainsDiagnosisName(String expectedName) throws Exception {
        String json = latestResponse.andReturn().getResponse().getContentAsString();
        assertThat(json).contains(expectedName);
    }

    @And("the response contains error code {string}")
    public void responseContainsErrorCode(String code) throws Exception {
        String json = latestResponse.andReturn().getResponse().getContentAsString();
        assertThat(json).contains(code);
    }
}
