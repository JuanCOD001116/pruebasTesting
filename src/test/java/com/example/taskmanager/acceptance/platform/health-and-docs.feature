@platform
Feature: Salud de la plataforma y documentación OpenAPI
  Como operador del sistema
  quiero verificar que la aplicación está saludable y expone su documentación
  para garantizar su operatividad y seguridad de endpoints de diagnóstico.

  Background:
    * url baseUrl

  @smoke @platform @H-04
  Scenario: A-52 GET /actuator/health verifica que la API está viva y saludable
    Given path 'actuator', 'health'
    When method get
    Then status 200
    And match response.status == 'UP'

  @platform @H-04
  Scenario Outline: A-53 Actuator solo expone health y no endpoints sensibles (<endpoint>)
    Given path 'actuator', '<endpoint>'
    When method get
    Then status 404

    Examples:
      | endpoint |
      | env      |
      | beans    |

  @platform
  Scenario: A-54 GET /v3/api-docs documenta los endpoints principales del sistema
    Given path 'v3', 'api-docs'
    When method get
    Then status 200
    * def rawDocs = typeof response == 'string' ? response : JSON.stringify(response)
    And match rawDocs contains '/api/tasks'
    And match rawDocs contains '/api/tasks/search'
    And match rawDocs contains '/api/tasks/summary'
    And match rawDocs contains '/api/tasks/{id}/status'
