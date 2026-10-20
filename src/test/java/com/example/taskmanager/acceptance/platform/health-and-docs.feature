@platform
Feature: Salud de la plataforma y documentación OpenAPI
  Como operador del sistema
  quiero verificar que la aplicación está saludable y expone su documentación
  para garantizar su operatividad.

  Background:
    * url baseUrl

  @smoke @platform @H-04
  Scenario: GET /actuator/health verifica que la API está viva y saludable
    Given path 'actuator', 'health'
    When method get
    Then status 200
    And match response.status == 'UP'
