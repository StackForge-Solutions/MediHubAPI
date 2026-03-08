package com.MediHubAPI.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.MediHubAPI.service.DiagnosisService;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for mock MVC tests
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @MockBean
    private DiagnosisService diagnosisService;
}
