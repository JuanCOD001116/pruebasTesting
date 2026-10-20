@status
Feature: Máquina de estados de una tarea
  Como usuario del gestor de tareas
  quiero que el sistema solo permita cambios de estado válidos
  para que el ciclo de vida de una tarea sea consistente.

  Background:
    * url apiUrl
    * def schemas = read('classpath:com/example/taskmanager/acceptance/common/schemas.json')
    * def createTask = read('classpath:com/example/taskmanager/acceptance/common/create-task.feature')
    * def changeStatus = read('classpath:com/example/taskmanager/acceptance/common/change-status.feature')

  @smoke @status
  Scenario Outline: A-16 Transición permitida de <desde> a <hacia>
    Given def created = call createTask { assignedTo: 'ana' }
    And def id = created.task.id
    And call changeStatus { id: '#(id)', path: <ruta> }
    When path id, 'status'
    And request { status: '<hacia>' }
    And method patch
    Then status 200
    And match response.status == '<hacia>'
    Given path id
    When method get
    Then status 200
    And match response.status == '<hacia>'

    Examples:
      | desde       | hacia       | ruta                |
      | PENDING     | IN_PROGRESS | []                  |
      | PENDING     | CANCELLED   | []                  |
      | IN_PROGRESS | PENDING     | ['IN_PROGRESS']     |
      | IN_PROGRESS | DONE        | ['IN_PROGRESS']     |
      | IN_PROGRESS | CANCELLED   | ['IN_PROGRESS']     |

  @status
  Scenario Outline: A-17 Transición prohibida de <desde> a <hacia>
    Given def created = call createTask { assignedTo: 'ana' }
    And def id = created.task.id
    And call changeStatus { id: '#(id)', path: <ruta> }
    When path id, 'status'
    And request { status: '<hacia>' }
    And method patch
    Then status 400
    And match response == schemas.error
    And match response.code == 400
    And match response.message contains 'inválida'
    Given path id
    When method get
    Then status 200
    And match response.status == '<desde>'

    Examples:
      | desde     | hacia       | ruta                     |
      | PENDING   | DONE        | []                       |
      | DONE      | PENDING     | ['IN_PROGRESS', 'DONE']  |
      | DONE      | IN_PROGRESS | ['IN_PROGRESS', 'DONE']  |
      | DONE      | CANCELLED   | ['IN_PROGRESS', 'DONE']  |
      | CANCELLED | PENDING     | ['CANCELLED']            |
      | CANCELLED | IN_PROGRESS | ['CANCELLED']            |
      | CANCELLED | DONE        | ['CANCELLED']            |

  @status
  Scenario Outline: A-18 Transición inválida al mismo estado (<desde> -> <desde>)
    Given def created = call createTask { assignedTo: 'ana' }
    And def id = created.task.id
    And call changeStatus { id: '#(id)', path: <ruta> }
    When path id, 'status'
    And request { status: '<desde>' }
    And method patch
    Then status 400
    And match response == schemas.error
    And match response.code == 400
    And match response.message contains 'mismo estado'
    Given path id
    When method get
    Then status 200
    And match response.status == '<desde>'

    # DONE y CANCELLED completan la matriz del spec 01 (TC-12 y TC-17): regla 3 antes que 1/2
    Examples:
      | desde       | ruta                    |
      | PENDING     | []                      |
      | IN_PROGRESS | ['IN_PROGRESS']         |
      | DONE        | ['IN_PROGRESS', 'DONE'] |
      | CANCELLED   | ['CANCELLED']           |

  @status
  Scenario: A-19 PENDING a IN_PROGRESS sin responsable falla con 400
    Given def created = call createTask {}
    And def id = created.task.id
    When path id, 'status'
    And request { status: 'IN_PROGRESS' }
    And method patch
    Then status 400
    And match response == schemas.error
    And match response.code == 400
    And match response.message contains 'falta assignedTo'

  @status
  Scenario: A-20 PENDING a IN_PROGRESS con responsable en blanco ("   ") falla con 400
    Given def created = call createTask { assignedTo: '   ' }
    And def id = created.task.id
    When path id, 'status'
    And request { status: 'IN_PROGRESS' }
    And method patch
    Then status 400
    And match response == schemas.error
    And match response.code == 400
    And match response.message contains 'falta assignedTo'

  @status @urgency
  Scenario: A-21 Ciclo de vida completo: crear -> iniciar -> pausar -> iniciar -> terminar
    Given def created = call createTask { assignedTo: 'maria' }
    And def id = created.task.id
    And match created.task.status == 'PENDING'
    When path id, 'status'
    And request { status: 'IN_PROGRESS' }
    And method patch
    Then status 200
    And match response.status == 'IN_PROGRESS'
    When path id, 'status'
    And request { status: 'PENDING' }
    And method patch
    Then status 200
    And match response.status == 'PENDING'
    When path id, 'status'
    And request { status: 'IN_PROGRESS' }
    And method patch
    Then status 200
    And match response.status == 'IN_PROGRESS'
    When path id, 'status'
    And request { status: 'DONE' }
    And method patch
    Then status 200
    And match response.status == 'DONE'
    Given path id
    When method get
    Then status 200
    And match response.status == 'DONE'
    And match response.urgency == 'NONE'

  @status @errors
  Scenario: A-22 Cambiar el estado de un id inexistente responde 404
    Given path '99999999', 'status'
    And request { status: 'IN_PROGRESS' }
    When method patch
    Then status 404
    And match response == schemas.error
    And match response.code == 404
