@summary
Feature: Resumen estadístico de tareas (F3)
  Como gestor del proyecto
  quiero consultar métricas agregadas y tasas de completitud
  para supervisar el estado global de las tareas.

  Background:
    * url apiUrl
    * def schemas = read('classpath:com/example/taskmanager/acceptance/common/schemas.json')
    * def createTask = read('classpath:com/example/taskmanager/acceptance/common/create-task.feature')
    * def changeStatus = read('classpath:com/example/taskmanager/acceptance/common/change-status.feature')

  @smoke @summary
  Scenario: A-46 El resumen contiene todas las claves de estado y prioridad
    Given path 'summary'
    When method get
    Then status 200
    And match response == schemas.summary
    And match response.byStatus contains { PENDING: '#number', IN_PROGRESS: '#number', DONE: '#number', CANCELLED: '#number' }
    And match response.byPriority contains { LOW: '#number', MEDIUM: '#number', HIGH: '#number' }
    And match response.total == '#number'
    And match response.overdue == '#number'
    And match response.completionRate == '#number'

  @summary
  Scenario: A-47 Métricas agregadas se incrementan con exactitud al crear tareas (2 HIGH PENDING y 1 LOW DONE)
    # 1. Medición antes
    Given path 'summary'
    When method get
    Then status 200
    * def before = response

    # 2. Creación de datos
    Given def c1 = call createTask { priority: 'HIGH' }
    And def c2 = call createTask { priority: 'HIGH' }
    And def c3 = call createTask { priority: 'LOW', assignedTo: 'laura' }
    And call changeStatus { id: '#(c3.task.id)', path: ['IN_PROGRESS', 'DONE'] }

    # 3. Medición después y comprobación por diferencias
    Given path 'summary'
    When method get
    Then status 200
    * def after = response
    And match after.total == before.total + 3
    And match after.byStatus.PENDING == before.byStatus.PENDING + 2
    And match after.byStatus.DONE == before.byStatus.DONE + 1
    And match after.byPriority.HIGH == before.byPriority.HIGH + 2
    And match after.byPriority.LOW == before.byPriority.LOW + 1

  @summary
  Scenario: A-48 completionRate es consistente con la fórmula DONE / (total - CANCELLED) * 100
    # Datos propios: garantizan un denominador > 0 aunque el escenario corra solo
    Given def finished = call createTask { priority: 'MEDIUM', assignedTo: 'sofia' }
    And call changeStatus { id: '#(finished.task.id)', path: ['IN_PROGRESS', 'DONE'] }
    And call createTask { priority: 'LOW' }
    Given path 'summary'
    When method get
    Then status 200
    * def total = response.total
    * def done = response.byStatus.DONE
    * def cancelled = response.byStatus.CANCELLED
    * def rate = response.completionRate
    And assert rate >= 0 && rate <= 100
    # A lo sumo 2 decimales (BigDecimal con escala 2, HALF_UP)
    And match ('' + rate) == '#regex \\d{1,3}(\\.\\d{1,2})?'
    * def denominator = total - cancelled
    And assert denominator > 0
    * def expectedRate = denominator == 0 ? 0.0 : (Math.round((done / denominator) * 10000) / 100)
    And assert Math.abs(rate - expectedRate) < 0.02

  @summary
  Scenario: A-49 GET /api/tasks/summary no se confunde con GET /api/tasks/{id}
    Given path 'summary'
    When method get
    Then status 200
    And match response == schemas.summary
    And match response.total == '#number'
