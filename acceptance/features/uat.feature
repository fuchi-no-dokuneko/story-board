@daily @uat @web
Feature: Daily acceptance of the StoryBlock operator console
  A human or browser agent verifies each visible request control, its response
  states, authentication boundary, and recovery without changing stored novels.

  Scenario: Load the console and observe the live API state
    Given I open the web application at path "/"
    Then the web page title contains "StoryBlock Console"
    And CSS "header h1" has text "StoryBlock"
    And CSS "header p" has text "Immutable narrative engine"
    And CSS "#status-text" eventually contains text "API online"
    And CSS "#response" has text "Ready."
    And CSS "#method" has value "GET"
    And CSS "#path" has value "/actuator/health"
    And CSS "#token" has attribute "type" equal to "password"
    And exactly 3 elements match CSS "#method option"
    And JavaScript expression "Array.from(document.querySelectorAll('#method option')).map((item) => item.value).join(',') === 'GET,POST,DELETE'" returns true

  Scenario: Send the public health quick request and pretty-print JSON
    Given I open the web application at path "/"
    When I click CSS "nav button[data-path='/actuator/health']"
    Then CSS "#method" has value "GET"
    And CSS "#path" has value "/actuator/health"
    And CSS "#response-status" eventually contains text "200"
    And CSS "#response" eventually contains text "\"status\": \"UP\""
    And JavaScript expression "document.getElementById('response').textContent.split(String.fromCharCode(10)).length > 2" returns true

  Scenario: Mark a reachable unhealthy API as degraded
    Given the next page load reports degraded API health
    When I open the web application at path "/"
    Then CSS "#status-text" eventually contains text "API degraded"
    And CSS "#status-dot" has attribute "class" equal to "down"

  Scenario: Mark an unreachable API health request as unavailable
    Given the next page load cannot reach API health
    When I open the web application at path "/"
    Then CSS "#status-text" eventually contains text "API unavailable"
    And CSS "#status-dot" has attribute "class" equal to "down"

  Scenario: Open the versioned API contract as readable plain text
    Given I open the web application at path "/"
    When I click CSS "nav button[data-path='/v1/openapi.yaml']"
    Then CSS "#method" has value "GET"
    And CSS "#path" has value "/v1/openapi.yaml"
    And CSS "#response-status" eventually contains text "200"
    And CSS "#response" eventually contains text "openapi:"
    And CSS "#response" contains text "/novels:"

  Scenario: Reject a protected request with no bearer credential
    Given I open the web application at path "/"
    When I choose value "POST" in CSS "#method"
    And I replace CSS "#path" with "/v1/novels"
    And I replace CSS "#body" with "{}"
    And I click CSS "#send"
    Then CSS "#response-status" eventually contains text "401"
    And CSS "#response" eventually contains text "AUTHENTICATION_REQUIRED"
    And CSS "#response" contains text "\"status\": 401"

  Scenario: Reject an invalid bearer credential without exposing it
    Given I open the web application at path "/"
    When I replace CSS "#path" with "/v1/novels/nov_missing"
    And I replace CSS "#token" with "invalid-uat-credential"
    And I click CSS "#send"
    Then CSS "#response-status" eventually contains text "401"
    And CSS "#response" eventually contains text "INVALID_BEARER_CREDENTIAL"
    And CSS "#response" contains text "The bearer credential is invalid, expired, or revoked."
    And JavaScript expression "!document.getElementById('response').textContent.includes(document.getElementById('token').value)" returns true

  Scenario: Send an authenticated POST body and expose missing mutation preconditions
    Given I open the web application at path "/"
    When I choose value "POST" in CSS "#method"
    And I replace CSS "#path" with "/v1/novels"
    And I replace CSS "#token" with environment variable "STORYBLOCK_UAT_OWNER_TOKEN"
    And I replace CSS "#body" with:
      """
      {"initial_revision":{}}
      """
    And I click CSS "#send"
    Then CSS "#response-status" eventually contains text "428"
    And CSS "#response" eventually contains text "IDEMPOTENCY_KEY_REQUIRED"
    And JavaScript expression "!document.getElementById('response').textContent.includes(document.getElementById('token').value)" returns true

  Scenario: Send DELETE and show its typed precondition failure
    Given I open the web application at path "/"
    When I choose value "DELETE" in CSS "#method"
    And I replace CSS "#path" with "/v1/access-keys/key_missing"
    And I replace CSS "#token" with environment variable "STORYBLOCK_UAT_OWNER_TOKEN"
    And I replace CSS "#body" with "{}"
    And I click CSS "#send"
    Then CSS "#response-status" eventually contains text "428"
    And CSS "#response" eventually contains text "IDEMPOTENCY_KEY_REQUIRED"

  Scenario: Recover a request after a temporary browser network failure
    Given I open the web application at path "/"
    Then CSS "#status-text" eventually contains text "API online"
    When I set the browser network offline
    And I click CSS "nav button[data-path='/actuator/health']"
    Then CSS "#response-status" eventually contains text "Network error"
    And CSS "#response" eventually contains text "Failed to fetch"
    When I restore the browser network
    And I click CSS "nav button[data-path='/actuator/health']"
    Then CSS "#response-status" eventually contains text "200"
    And CSS "#response" eventually contains text "\"status\": \"UP\""

  Scenario: Reload clears sensitive and unsent request edits
    Given I open the web application at path "/"
    When I choose value "DELETE" in CSS "#method"
    And I replace CSS "#path" with "/temporary-path"
    And I replace CSS "#token" with "temporary-secret"
    And I replace CSS "#body" with "{\"temporary\":true}"
    And I reload the web page
    Then CSS "#method" has value "GET"
    And CSS "#path" has value "/actuator/health"
    And CSS "#token" has value ""
    And CSS "#body" has value "{}"
    And CSS "#response" has text "Ready."
