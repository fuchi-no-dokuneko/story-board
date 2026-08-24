@demo @recording @english @web
Feature: English introduction to StoryBlock Console
  The recording introduces the product and demonstrates the main request flow
  without changing narrative data.

  Scenario: Introduce the console and inspect its API safely
    Given I begin a recorded demo
    And I open the web application at path "/"
    When I narrate in "en-US" for at least 9 seconds:
      """
      StoryBlock is a local-first engine for immutable, evidence-bound editing of long-form narrative blocks. This compact operator console gives an operator a direct, observable window into that API.
      """
    Then CSS "#status-text" eventually contains text "API online"
    When I narrate in "en-US" for at least 7 seconds:
      """
      The status at the top confirms whether the API is available. We can begin with the public health request and inspect the exact HTTP result below.
      """
    And I click CSS "nav button[data-path='/actuator/health']"
    Then CSS "#response-status" eventually contains text "200"
    And CSS "#response" eventually contains text "\"status\": \"UP\""
    When I narrate in "en-US" for at least 8 seconds:
      """
      JSON responses are formatted for review, while the OpenAPI shortcut opens the versioned contract as readable YAML. This is the quickest way to discover supported operations.
      """
    And I click CSS "nav button[data-path='/v1/openapi.yaml']"
    Then CSS "#response" eventually contains text "openapi:"
    And CSS "#response" contains text "/novels:"
    When I narrate in "en-US" for at least 9 seconds:
      """
      For a custom request, choose GET, POST, or DELETE, enter the path and JSON body, and provide a bearer token only when the route requires one. Tokens remain masked and are never copied into the response.
      """
    And I choose value "POST" in CSS "#method"
    And I replace CSS "#path" with "/v1/novels"
    And I replace CSS "#body" with "{}"
    And I click CSS "#send"
    Then CSS "#response-status" eventually contains text "401"
    And CSS "#response" eventually contains text "AUTHENTICATION_REQUIRED"
    When I narrate in "en-US" for at least 8 seconds:
      """
      The typed unauthorized response is expected here and demonstrates the fail-closed boundary. Operators can diagnose status, error code, and request details without exposing a credential or mutating a novel.
      """
    Then I finish the recorded demo
