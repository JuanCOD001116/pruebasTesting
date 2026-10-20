@ignore
Feature: Paso individual para cambio de estado

  Scenario:
    * def expected = karate.get('__arg.expectedStatus', 200)
    Given url apiUrl + '/' + __arg.id + '/status'
    And request { status: '#(__arg.status)' }
    When method patch
    Then status expected
    * def result = response
