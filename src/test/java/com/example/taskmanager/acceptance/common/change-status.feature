@ignore
Feature: Helper — cambiar estado de una tarea

  Scenario:
    * def taskId = __arg.id
    * def expected = karate.get('__arg.expectedStatus', 200)
    * def statuses = karate.get('__arg.path', null) != null ? __arg.path : (__arg.status ? [__arg.status] : [])
    * def step = read('classpath:com/example/taskmanager/acceptance/common/patch-status-step.feature')
    * def loopArgs = karate.map(statuses, function(s){ return { id: taskId, status: s, expectedStatus: expected } })
    * call step loopArgs
