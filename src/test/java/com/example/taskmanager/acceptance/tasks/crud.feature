@crud
Feature: Operaciones CRUD de tareas
  Como usuario del gestor de tareas
  quiero crear, consultar, actualizar y eliminar tareas
  para gestionar el ciclo de trabajo básico.

  Background:
    * url apiUrl
    * def schemas = read('classpath:com/example/taskmanager/acceptance/common/schemas.json')
    * def createTask = read('classpath:com/example/taskmanager/acceptance/common/create-task.feature')

  @smoke @crud
  Scenario: A-01 Crear una tarea con datos mínimos
    Given request { title: '#(uniqueTitle("Tarea Mínima"))' }
    When method post
    Then status 201
    And match response == schemas.task
    And match response.status == 'PENDING'
    And match response.priority == 'MEDIUM'

  @crud
  Scenario: A-02 Crear con todos los campos (descripción, prioridad, responsable, fecha límite)
    * def taskTitle = uniqueTitle("Tarea Completa")
    * def taskDueDate = plusDays(5)
    Given request
      """
      {
        title: '#(taskTitle)',
        description: 'Descripción detallada',
        priority: 'HIGH',
        assignedTo: 'carlos',
        dueDate: '#(taskDueDate)'
      }
      """
    When method post
    Then status 201
    And match response == schemas.task
    And match response.title == taskTitle
    And match response.description == 'Descripción detallada'
    And match response.priority == 'HIGH'
    And match response.assignedTo == 'carlos'
    And match response.dueDate == taskDueDate
    And match response.status == 'PENDING'

  @smoke @crud
  Scenario: A-03 Consultar por id una tarea recién creada
    Given def created = call createTask { description: 'Para consultar', priority: 'LOW' }
    And def taskId = created.task.id
    Given path taskId
    When method get
    Then status 200
    And match response == created.task

  @crud @errors
  Scenario: A-04 Consultar un id inexistente
    Given path '99999999'
    When method get
    Then status 404
    And match response == schemas.error
    And match response.code == 404
    And match response.message contains '99999999'

  @crud
  Scenario: A-05 Actualizar título y prioridad con PUT
    Given def created = call createTask { priority: 'LOW' }
    And def taskId = created.task.id
    * def newTitle = uniqueTitle("Tarea Actualizada")
    Given path taskId
    And request { title: '#(newTitle)', priority: 'HIGH' }
    When method put
    Then status 200
    And match response.id == taskId
    And match response.title == newTitle
    And match response.priority == 'HIGH'
    Given path taskId
    When method get
    Then status 200
    And match response.title == newTitle
    And match response.priority == 'HIGH'

  @crud
  Scenario: A-06 PUT parcial: solo se envía la descripción
    * def originalTitle = uniqueTitle("Tarea para PUT parcial")
    * def originalDueDate = plusDays(4)
    Given def created = call createTask { title: '#(originalTitle)', priority: 'HIGH', assignedTo: 'maria', dueDate: '#(originalDueDate)' }
    And def taskId = created.task.id
    Given path taskId
    And request { description: 'Solo descripción modificada' }
    When method put
    Then status 200
    And match response.id == taskId
    And match response.description == 'Solo descripción modificada'
    And match response.title == originalTitle
    And match response.priority == 'HIGH'
    And match response.assignedTo == 'maria'
    And match response.dueDate == originalDueDate
    And match response.status == 'PENDING'

  @crud
  Scenario: A-07 Eliminar una tarea
    Given def created = call createTask {}
    And def taskId = created.task.id
    Given path taskId
    When method delete
    Then status 204
    Given path taskId
    When method get
    Then status 404
    And match response == schemas.error
    And match response.code == 404

  @crud @errors
  Scenario: A-08 Eliminar un id inexistente
    Given path '99999999'
    When method delete
    Then status 404
    And match response == schemas.error
    And match response.code == 404

  @crud
  Scenario: A-09 Listar tareas (GET /api/tasks)
    * def taskTitle = uniqueTitle("Tarea Para Listar")
    Given def created = call createTask { title: '#(taskTitle)' }
    And def taskId = created.task.id
    When method get
    Then status 200
    And match response == '#[]'
    And match response contains deep { id: '#(taskId)', title: '#(taskTitle)' }
