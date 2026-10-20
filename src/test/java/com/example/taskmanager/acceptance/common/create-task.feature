@ignore
Feature: Helper — crear una tarea

  Scenario:
    * def defaults = { title: '#(uniqueTitle("Tarea"))', priority: 'MEDIUM' }
    * def body = karate.merge(defaults, __arg)
    Given url apiUrl
    And request body
    When method post
    Then status 201
    * def task = response
