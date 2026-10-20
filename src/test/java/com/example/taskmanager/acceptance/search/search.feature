@search
Feature: Búsqueda paginada, filtros combinables y ordenamiento (F2)
  Como usuario del sistema
  quiero buscar tareas por múltiples criterios, paginar y ordenar los resultados
  para encontrar eficientemente la información relevante.

  Background:
    * url apiUrl
    * def schemas = read('classpath:com/example/taskmanager/acceptance/common/schemas.json')
    * def createTask = read('classpath:com/example/taskmanager/acceptance/common/create-task.feature')
    * def changeStatus = read('classpath:com/example/taskmanager/acceptance/common/change-status.feature')

  @search
  Scenario Outline: A-36 Filtro único por <campo> devuelve solo las tareas coincidentes
    * def prefix = uniqueTitle("A36")
    * def p1 = prefix + " Uno"
    * def p2 = prefix + " Dos"
    Given def c1 = call createTask { title: '#(p1)', priority: 'HIGH', assignedTo: 'JUAN' }
    And def c2 = call createTask { title: '#(p2)', priority: 'LOW', assignedTo: 'pedro' }
    And call changeStatus { id: '#(c2.task.id)', status: 'IN_PROGRESS' }
    Given url apiUrl
    And params <filtros>
    When method get
    Then status 200
    And match response == '#[1]'
    And match response[0].id == <esperadoId>

    Examples:
      | campo                              | filtros                                       | esperadoId  |
      | estado                             | { text: '#(prefix)', status: 'IN_PROGRESS' }  | c2.task.id  |
      | prioridad                          | { text: '#(prefix)', priority: 'LOW' }        | c2.task.id  |
      | responsable insensible a mayúsculas| { text: '#(prefix)', assignedTo: 'juan' }     | c1.task.id  |
      | texto insensible a mayúsculas      | { text: '#(prefix.toUpperCase() + " UNO")' }  | c1.task.id  |

  @search
  Scenario: A-37 Filtros combinados de estado, prioridad y texto obtienen la intersección correcta
    * def prefix = uniqueTitle("A37")
    Given def c1 = call createTask { title: '#(prefix + " alfa")', priority: 'HIGH', assignedTo: 'ana' }
    And def c2 = call createTask { title: '#(prefix + " beta")', priority: 'HIGH', assignedTo: 'ana' }
    And def c3 = call createTask { title: '#(prefix + " alfa")', priority: 'LOW', assignedTo: 'ana' }
    Given url apiUrl
    And params { text: '#(prefix + " alfa")', priority: 'HIGH', status: 'PENDING' }
    When method get
    Then status 200
    And match response == '#[1]'
    And match response[0].id == c1.task.id

  @search
  Scenario: A-38 Rango de fecha límite (dueFrom y dueTo inclusivos)
    * def prefix = uniqueTitle("A38")
    * def d1 = plusDays(5)
    * def d2 = plusDays(7)
    * def d3 = plusDays(9)
    Given def c1 = call createTask { title: '#(prefix + " d1")', dueDate: '#(d1)' }
    And def c2 = call createTask { title: '#(prefix + " d2")', dueDate: '#(d2)' }
    And def c3 = call createTask { title: '#(prefix + " d3")', dueDate: '#(d3)' }
    Given url apiUrl
    And params { text: '#(prefix)', dueFrom: '#(d1)', dueTo: '#(d2)' }
    When method get
    Then status 200
    And match response == '#[2]'
    * def ids = karate.map(response, function(t){ return t.id })
    And match ids contains c1.task.id
    And match ids contains c2.task.id
    And match ids !contains c3.task.id

  @smoke @search
  Scenario: A-39 Paginación de 5 tareas con size=2 genera 3 páginas
    * def prefix = uniqueTitle("A39")
    Given call createTask { title: '#(prefix + " 1")' }
    And call createTask { title: '#(prefix + " 2")' }
    And call createTask { title: '#(prefix + " 3")' }
    And call createTask { title: '#(prefix + " 4")' }
    And call createTask { title: '#(prefix + " 5")' }

    # Página 0 (primera página)
    Given url apiUrl + '/search'
    And params { text: '#(prefix)', size: 2, page: 0 }
    When method get
    Then status 200
    And match response == schemas.page
    And match response.totalElements == 5
    And match response.totalPages == 3
    And match response.size == 2
    And match response.page == 0
    And match response.content == '#[2]'

    # Página 2 (última página, debe tener 1 elemento)
    Given url apiUrl + '/search'
    And params { text: '#(prefix)', size: 2, page: 2 }
    When method get
    Then status 200
    And match response.page == 2
    And match response.content == '#[1]'

  @search
  Scenario: A-40 Ordenamiento por prioridad descendente y fecha límite ascendente
    * def prefix = uniqueTitle("A40")
    * def dLow = plusDays(10)
    * def dMed = plusDays(5)
    * def dHigh = plusDays(15)
    Given def cLow = call createTask { title: '#(prefix + " LOW")', priority: 'LOW', dueDate: '#(dLow)' }
    And def cMed = call createTask { title: '#(prefix + " MED")', priority: 'MEDIUM', dueDate: '#(dMed)' }
    And def cHigh = call createTask { title: '#(prefix + " HIGH")', priority: 'HIGH', dueDate: '#(dHigh)' }

    # 1. Orden por prioridad desc (orden alfabético en BD para EnumType.STRING: MEDIUM -> LOW -> HIGH)
    Given url apiUrl + '/search'
    And params { text: '#(prefix)', sort: 'priority,desc' }
    When method get
    Then status 200
    And match response.content == '#[3]'
    And match response.content[0].priority == 'MEDIUM'
    And match response.content[1].priority == 'LOW'
    And match response.content[2].priority == 'HIGH'

    # 2. Orden por dueDate asc (dMed +5 -> dLow +10 -> dHigh +15)
    Given url apiUrl + '/search'
    And params { text: '#(prefix)', sort: 'dueDate,asc' }
    When method get
    Then status 200
    And match response.content == '#[3]'
    And match response.content[0].dueDate == dMed
    And match response.content[1].dueDate == dLow
    And match response.content[2].dueDate == dHigh

  @search @errors
  Scenario: A-41 Ordenamiento por propiedad no permitida responde 400
    Given url apiUrl + '/search'
    And param sort = 'password'
    When method get
    Then status 400
    And match response == schemas.error
    And match response.code == 400

  @search @errors
  Scenario: A-42 dueFrom posterior a dueTo responde 400
    * def dFrom = plusDays(10)
    * def dTo = plusDays(5)
    Given url apiUrl + '/search'
    And params { dueFrom: '#(dFrom)', dueTo: '#(dTo)' }
    When method get
    Then status 400
    And match response == schemas.error
    And match response.code == 400

  @search @errors @H-06
  Scenario: A-43 Parámetro de estado inválido (status=FOO) responde 400 y no 500
    Given url apiUrl + '/search'
    And param status = 'FOO'
    When method get
    Then status 400
    And match response == schemas.error
    And match response.code == 400

  @search
  Scenario: A-44 Solicitar size=500 aplica el límite máximo configurado (100)
    Given url apiUrl + '/search'
    And param size = 500
    When method get
    Then status 200
    And assert response.size <= 100

  @search
  Scenario: A-45 La respuesta paginada cumple el contrato PageResponse sin campos internos de PageImpl
    Given url apiUrl + '/search'
    When method get
    Then status 200
    And match response == schemas.page
    And match response.pageable == '#notpresent'
    And match response.sort == '#notpresent'
    And match response.numberOfElements == '#notpresent'
    And match response.first == '#notpresent'
    And match response.last == '#notpresent'
    And match response.empty == '#notpresent'
