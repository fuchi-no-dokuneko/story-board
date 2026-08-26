@daily @uat @web
Feature: Daily acceptance of the StoryBlock read-only novel library
  A human or browser agent verifies persisted novel text and metadata through
  the trusted-LAN admin UI without changing stored content.

  Scenario: Load the read-only library and observe the live service
    Given I open the web application at path "/"
    Then the web page title contains "StoryBlock Library"
    And CSS ".brand h1" has text "StoryBlock"
    And CSS ".brand p" has text "Novel library"
    And CSS ".readonly-badge" has text "Read only"
    And CSS "#status-text" eventually contains text "Service online"
    And CSS "#catalog-total" eventually contains text "persisted novel"
    And no elements match CSS "#library-view textarea"
    And no elements match CSS "#library-view [contenteditable='true']"
    And no elements match CSS "button[data-method='POST'], button[data-method='DELETE']"

  Scenario: Expose one shared non-persistent operator credential control
    Given I open the web application at path "/"
    Then CSS "#operator-token" is visible
    And CSS "#operator-connect" has text "Use token"
    And CSS "#operator-status" eventually contains text "access active"
    And local storage item "storyblock.operatorToken" is absent
    And JavaScript expression "sessionStorage.getItem('storyblock.operatorToken') === null" returns true
    When I click CSS "#console-tab"
    Then CSS ".console-auth-note" contains text "owner token configured above"

  Scenario: Read and count the independently registered Minecraft novel
    Given I open the web application at path "/"
    When I replace CSS "#catalog-search" with environment variable "STORYBLOCK_UAT_NOVEL_TITLE"
    Then exactly 1 elements match CSS ".novel-item"
    When I click CSS ".novel-item"
    Then CSS "#reader-content" is visible
    And JavaScript expression "document.getElementById('reader-empty').offsetParent === null && document.getElementById('reader-loading').offsetParent === null && document.getElementById('reader-content').offsetParent !== null" returns true
    And CSS "#reader-title" contains environment variable "STORYBLOCK_UAT_NOVEL_TITLE"
    And CSS "#reader-id" contains environment variable "STORYBLOCK_UAT_NOVEL_ID"
    And CSS "#reader-registration" has text "Agent write registered"
    And CSS "#stat-han" has text "10,000"
    And CSS "#stat-zombies" has text "1,000"
    And CSS "#stat-cannons" has text "1,000"
    And exactly 5 elements match CSS "#character-list li"
    And at least 2 elements match CSS "#chapter-nav .chapter-link"
    And at least 100 elements match CSS ".story-block"
    And CSS "#chapter-content" contains environment variable "STORYBLOCK_UAT_TEXT_MARKER"
    And JavaScript expression "(() => { const han = new RegExp(String.fromCharCode(92) + 'p{Script=Han}', 'u'); return Array.from(Array.from(document.querySelectorAll('.story-block')).map((item) => item.textContent).join('')).filter((character) => han.test(character)).length === 10000; })()" returns true
    And JavaScript expression "document.getElementById('meta-han-hash').textContent.startsWith('sha256:')" returns true

  Scenario: Search the catalog by a main-character name
    Given I open the web application at path "/"
    When I replace CSS "#catalog-search" with environment variable "STORYBLOCK_UAT_CHARACTER"
    Then exactly 1 elements match CSS ".novel-item"
    And CSS ".novel-item" contains environment variable "STORYBLOCK_UAT_NOVEL_TITLE"

  Scenario: Navigate between persisted chapters
    Given I open the web application at path "/"
    Then CSS "#reader-content" is visible
    And JavaScript expression "document.getElementById('reader-empty').offsetParent === null && document.getElementById('reader-loading').offsetParent === null" returns true
    And at least 2 elements match CSS "#chapter-nav .chapter-link"
    When I click CSS "#chapter-nav .chapter-link:nth-child(2)"
    Then CSS "#chapter-2" is visible
    And JavaScript expression "document.querySelector('#chapter-2 .story-block').textContent.length > 0" returns true

  Scenario: Use the read-only API diagnostics
    Given I open the web application at path "/"
    When I click CSS "#console-tab"
    Then CSS "#console-view" is visible
    And exactly 1 elements match CSS "#method option"
    And CSS "#method" has value "GET"
    When I click CSS ".quick-requests button[data-path='/actuator/health']"
    Then CSS "#response-status" eventually contains text "200"
    And CSS "#response" eventually contains text "\"status\": \"UP\""
    When I click CSS ".quick-requests button[data-path='/v1/openapi.yaml']"
    Then CSS "#response" eventually contains text "openapi:"
    And CSS "#response" contains text "/admin/novels:"

  Scenario: Distinguish degraded and unavailable service states
    Given the next page load reports degraded API health
    When I open the web application at path "/"
    Then CSS "#status-text" eventually contains text "Service degraded"
    And CSS "#status-dot" has attribute "class" equal to "down"

  Scenario: Recover diagnostics after a temporary browser network failure
    Given I open the web application at path "/"
    Then CSS "#status-text" eventually contains text "Service online"
    When I click CSS "#console-tab"
    And I set the browser network offline
    And I click CSS ".quick-requests button[data-path='/actuator/health']"
    Then CSS "#response-status" eventually contains text "Network error"
    When I restore the browser network
    And I click CSS ".quick-requests button[data-path='/actuator/health']"
    Then CSS "#response-status" eventually contains text "200"
    And CSS "#response" eventually contains text "\"status\": \"UP\""

  Scenario: Keep the reader usable on a narrow mobile viewport
    Given I open the web application at path "/"
    Then CSS "#reader-content" is visible
    When I resize the browser to 390 by 844
    Then CSS ".catalog-pane" is visible
    And CSS ".reader-pane" is visible
    And JavaScript expression "document.documentElement.scrollWidth <= document.documentElement.clientWidth" returns true
    And JavaScript expression "Array.from(document.querySelectorAll('.novel-stats dd')).every((item) => item.getBoundingClientRect().width > 0)" returns true
