@validation
Feature: Validación de entrada y manejo de errores de validación
  Como sistema
  quiero validar estrictamente las entradas de la API
  para rechazar solicitudes con datos inválidos o mal formados con HTTP 400.

  Background:
    * url apiUrl
    * def schemas = read('classpath:com/example/taskmanager/acceptance/common/schemas.json')
    * def createTask = read('classpath:com/example/taskmanager/acceptance/common/create-task.feature')

  @validation @R1
  Scenario Outline: A-10 Crear tarea sin título o con título en blanco falla con 400 (<descripcion>)
    Given request <payload>
    When method post
    Then status 400
    And match response == schemas.error
    And match response.code == 400

    Examples:
      | descripcion        | payload                   |
      | sin título         | { priority: 'LOW' }       |
      | título vacío       | { title: '' }             |
      | título con espacios| { title: '     ' }        |

  @validation
  Scenario: A-11 Crear tarea con título de 256 caracteres excede longitud máxima
    * def longTitle = 'a'.repeat(256)
    Given request { title: '#(longTitle)' }
    When method post
    Then status 400
    And match response == schemas.error
    And match response.code == 400

  @validation
  Scenario: A-12 Crear tarea con descripción de 1001 caracteres excede longitud máxima
    * def longDescription = 'd'.repeat(1001)
    Given request { title: '#(uniqueTitle("Tarea"))', description: '#(longDescription)' }
    When method post
    Then status 400
    And match response == schemas.error
    And match response.code == 400

  @validation @H-09
  Scenario: A-13 PUT con título en blanco es rechazado con 400
    Given def created = call createTask {}
    And def taskId = created.task.id
    Given path taskId
    And request { title: '   ' }
    When method put
    Then status 400
    And match response == schemas.error
    And match response.code == 400

  @validation @H-06
  Scenario Outline: A-14 Valor enum inexistente <caso> responde 400 y no 500
    Given def created = call createTask {}
    And def taskId = created.task.id
    * def targetPath = '<subpath>'.replace('{id}', taskId)
    Given url apiUrl + targetPath
    And request <payload>
    When method <metodo>
    Then status 400
    And match response == schemas.error
    And match response.code == 400

    Examples:
      | caso                     | metodo | subpath        | payload                                     |
      | prioridad inexistente    | post   |                | { title: 'Tarea Test', priority: 'URGENT' } |
      | estado inexistente patch | patch  | /{id}/status   | { status: 'ARCHIVED' }                      |

  @validation @H-06
  Scenario: A-15 JSON mal formado responde 400 y no 500
    Given header Content-Type = 'application/json'
    And request '{ "title": "Incompleto", '
    When method post
    Then status 400
    And match response == schemas.error
    And match response.code == 400
    And match response.message contains 'mal formado'
