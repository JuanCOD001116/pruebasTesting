@errors
Feature: Formato estandarizado y seguridad de respuestas de error (H-06)
  Como consumidor de la API
  quiero que todas las respuestas de error sigan una estructura homogénea
  y nunca expongan detalles internos de implementación ni stack traces.

  Background:
    * url apiUrl
    * def schemas = read('classpath:com/example/taskmanager/acceptance/common/schemas.json')

  @errors @H-06
  Scenario Outline: A-50 Errores <codigo> (<caso>) cumplen con el esquema estándar
    Given url apiUrl + '<subpath>'
    And header Content-Type = 'application/json'
    And request <payload>
    When method <metodo>
    Then status <codigo>
    And match response == schemas.error
    And match response.code == <codigo>
    And match response.message == '#string'
    And match response.timestamp == '#string'
    And match response.path == '#string'

    Examples:
      | caso                  | metodo | subpath         | payload             | codigo |
      | recurso inexistente   | get    | /99999999       | ''                  | 404    |
      | validación de entrada | post   |                 | { priority: 'LOW' } | 400    |
      | parámetro inválido    | get    | ?status=INVALID | ''                  | 400    |

  @errors @H-06
  Scenario: A-51 Las respuestas de error nunca exponen excepciones internas ni trazas
    # Error 404
    Given path '99999999'
    When method get
    Then status 404
    * def raw404 = typeof response == 'string' ? response : JSON.stringify(response)
    And match raw404 !contains 'Exception'
    And match raw404 !contains 'at com.example'
    And match raw404 !contains 'org.hibernate'
    And match raw404 !contains 'SQL'

    # Error 400 con JSON mal formado
    Given header Content-Type = 'application/json'
    And request '{ "title": "Incompleto", '
    When method post
    Then status 400
    * def raw400 = typeof response == 'string' ? response : JSON.stringify(response)
    And match raw400 !contains 'Exception'
    And match raw400 !contains 'at com.example'
    And match raw400 !contains 'org.hibernate'
    And match raw400 !contains 'SQL'
