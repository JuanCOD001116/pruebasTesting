# Limitación documentada — OVERDUE:
# Con la API no se puede crear ni actualizar una tarea con fecha límite pasada (R7, R8),
# y el reloj del sistema se encuentra fijo en 2030-01-15 durante las pruebas de aceptación.
# Por lo tanto, el estado OVERDUE y el filtro overdue=true no son alcanzables por HTTP
# en un entorno determinista sin modificar el código de producción. Esta lógica queda
# completamente garantizada a nivel unitario (TaskUrgencyEvaluatorTest, OverdueRule, TaskSpecificationsTest).
# En este feature, el escenario A-35 valida la parte alcanzable: GET /api/tasks?overdue=true
# responde 200 y no contiene las tareas creadas durante la ejecución.

@urgency
Feature: Urgencia calculada de tareas (R9)
  Como usuario del sistema
  quiero que la urgencia de las tareas se calcule automáticamente
  según el estado, la prioridad y la fecha límite para priorizar el trabajo.

  Background:
    * url apiUrl
    * def schemas = read('classpath:com/example/taskmanager/acceptance/common/schemas.json')
    * def createTask = read('classpath:com/example/taskmanager/acceptance/common/create-task.feature')
    * def changeStatus = read('classpath:com/example/taskmanager/acceptance/common/change-status.feature')

  @urgency
  Scenario: A-28 Tarea sin fecha límite y prioridad alta tiene urgencia AT_RISK
    Given def created = call createTask { priority: 'HIGH' }
    Then match created.task.urgency == 'AT_RISK'

  @urgency
  Scenario Outline: A-29 Tarea sin fecha límite y prioridad media o baja tiene urgencia ON_TRACK (<prioridad>)
    Given def created = call createTask { priority: '<prioridad>' }
    Then match created.task.urgency == 'ON_TRACK'

    Examples:
      | prioridad |
      | MEDIUM    |
      | LOW       |

  @urgency
  Scenario Outline: A-30 Tarea que vence hoy tiene urgencia AT_RISK (<prioridad>)
    * def todayDate = plusDays(0)
    Given def created = call createTask { priority: '<prioridad>', dueDate: '#(todayDate)' }
    Then match created.task.urgency == 'AT_RISK'

    Examples:
      | prioridad |
      | HIGH      |
      | MEDIUM    |
      | LOW       |

  @urgency
  Scenario Outline: A-31 Tarea que vence en el último día de la ventana de alerta tiene urgencia AT_RISK (<prioridad>)
    * def targetDate = plusDays(<dias>)
    Given def created = call createTask { priority: '<prioridad>', dueDate: '#(targetDate)' }
    Then match created.task.urgency == 'AT_RISK'

    Examples:
      | prioridad | dias |
      | HIGH      | 3    |
      | MEDIUM    | 2    |
      | LOW       | 1    |

  @urgency
  Scenario Outline: A-32 Tarea que vence el día siguiente a la ventana de alerta tiene urgencia ON_TRACK (<prioridad>)
    * def targetDate = plusDays(<dias>)
    Given def created = call createTask { priority: '<prioridad>', dueDate: '#(targetDate)' }
    Then match created.task.urgency == 'ON_TRACK'

    Examples:
      | prioridad | dias |
      | HIGH      | 4    |
      | MEDIUM    | 3    |
      | LOW       | 2    |

  @urgency
  Scenario Outline: A-33 Tarea en estado terminal (<estado>) con fecha cercana tiene urgencia NONE
    * def nearDate = plusDays(1)
    Given def created = call createTask { assignedTo: 'pedro', priority: 'HIGH', dueDate: '#(nearDate)' }
    And def taskId = created.task.id
    And call changeStatus { id: '#(taskId)', path: <ruta> }
    Given path taskId
    When method get
    Then status 200
    And match response.status == '<estado>'
    And match response.urgency == 'NONE'

    Examples:
      | estado    | ruta                   |
      | DONE      | ['IN_PROGRESS', 'DONE']|
      | CANCELLED | ['CANCELLED']          |

  @urgency
  Scenario: A-34 La urgencia aparece de forma consistente en GET individual, listado y búsqueda paginada
    * def taskTitle = uniqueTitle("Consistencia Urgencia")
    Given def created = call createTask { title: '#(taskTitle)', priority: 'HIGH' }
    And def taskId = created.task.id
    Then match created.task.urgency == 'AT_RISK'

    # 1. Consulta individual
    Given path taskId
    When method get
    Then status 200
    And match response.urgency == 'AT_RISK'

    # 2. Listado con filtro por texto
    Given url apiUrl
    And param text = taskTitle
    When method get
    Then status 200
    And match response == '#[1]'
    And match response[0].urgency == 'AT_RISK'

    # 3. Búsqueda paginada
    Given url apiUrl + '/search'
    And param text = taskTitle
    When method get
    Then status 200
    And match response == schemas.page
    And match response.content == '#[1]'
    And match response.content[0].urgency == 'AT_RISK'

  @urgency
  Scenario: A-35 Consulta con filtro overdue=true responde 200 y no incluye tareas activas vigentes
    * def taskTitle = uniqueTitle("Tarea No Vencida")
    Given def created = call createTask { title: '#(taskTitle)', priority: 'HIGH', dueDate: '#(plusDays(2))' }
    And def taskId = created.task.id
    Given url apiUrl
    And param overdue = true
    When method get
    Then status 200
    And match response == '#[]'
    * def ids = karate.map(response, function(t){ return t.id })
    And match ids !contains taskId
    # Con el reloj fijo y R7/R8 ninguna tarea creada en la ejecución puede estar vencida
    And match response == []
