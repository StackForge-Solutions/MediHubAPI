Feature: Diagnosis API

  Scenario: Create diagnosis successfully
    Given a diagnosis create response for appointment 1
    When I POST "/api/diagnoses" with appointmentId 1 and diagnosis payload
    Then the response status is 201
    And the response contains diagnosis name "Influenza"

  Scenario: Create diagnosis fails for future sinceYear
    Given diagnosis create will fail for appointment 1 with sinceYear in future
    When I POST "/api/diagnoses" with appointmentId 1 and diagnosis payload
    Then the response status is 400
    And the response contains error code "DIAGNOSIS_SINCE_YEAR_FUTURE"
