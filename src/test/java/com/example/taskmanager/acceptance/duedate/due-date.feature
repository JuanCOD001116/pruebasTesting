@duedate
Feature: Reglas de negocio para fecha límite (dueDate)
  Como gestor de tareas
  quiero validar que las fechas límite no puedan fijarse en el pasado
  para garantizar la coherencia temporal de las tareas según las reglas R7 y R8.

  Background:
    * url apiUrl
    * def schemas = read('classpath:com/example/taskmanager/acceptance/common/schemas.json')
    * def createTask = read('classpath:com/example/taskmanager/acceptance/common/create-task.feature')

  @duedate @R7
  Scenario: A-23 Crear tarea con fecha límite en el pasado falla con 400
    * def yesterday = plusDays(-1)
    Given request { title: '#(uniqueTitle("Tarea Ayer"))', dueDate: '#(yesterday)' }
    When method post
    Then status 400
    And match response == schemas.error
    And match response.code == 400

  @duedate @R7
  Scenario Outline: A-24 Crear con fecha límite permitida (<momento>) responde 201
    * def targetDate = plusDays(<dias>)
    * def taskTitle = uniqueTitle("Tarea Válida")
    Given request { title: '#(taskTitle)', dueDate: '#(targetDate)' }
    When method post
    Then status 201
    And match response == schemas.task
    And match response.dueDate == targetDate

    Examples:
      | momento          | dias |
      | hoy              | 0    |
      | mañana           | 1    |
      | dentro de 30 días| 30   |

  @duedate
  Scenario: A-25 Crear sin fecha límite guarda dueDate nulo
    Given request { title: '#(uniqueTitle("Sin Fecha"))' }
    When method post
    Then status 201
    And match response == schemas.task
    And match response.dueDate == '#null'

  @duedate @R8
  Scenario: A-26 PUT que cambia la fecha límite a una pasada falla con 400
    * def originalDate = plusDays(5)
    * def pastDate = plusDays(-1)
    Given def created = call createTask { dueDate: '#(originalDate)' }
    And def taskId = created.task.id
    Given path taskId
    And request { dueDate: '#(pastDate)' }
    When method put
    Then status 400
    And match response == schemas.error
    And match response.code == 400

  @duedate @R8
  Scenario: A-27 PUT que cambia la fecha límite a una futura válida responde 200
    * def originalDate = plusDays(5)
    * def newFutureDate = plusDays(10)
    Given def created = call createTask { dueDate: '#(originalDate)' }
    And def taskId = created.task.id
    Given path taskId
    And request { dueDate: '#(newFutureDate)' }
    When method put
    Then status 200
    And match response.id == taskId
    And match response.dueDate == newFutureDate
    Given path taskId
    When method get
    Then status 200
    And match response.dueDate == newFutureDate
