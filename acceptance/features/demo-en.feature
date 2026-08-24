@demo @recording @english @web
Feature: English introduction to the StoryBlock novel library
  The recording introduces read-only local review without changing narrative data.

  Scenario: Read an agent-authored novel and inspect its contract
    Given I begin a recorded demo
    And I open the web application at path "/"
    When I narrate in "en-US" for at least 8 seconds:
      """
      StoryBlock is a local-first library for immutable narrative revisions. This trusted-LAN screen lets an administrator review every persisted novel without exposing editing or deletion controls.
      """
    Then CSS "#status-text" eventually contains text "Service online"
    And CSS "#reader-content" is visible
    When I narrate in "en-US" for at least 9 seconds:
      """
      The catalog opens the current revision directly. The reader shows the title, five main characters, chapter navigation, and the exact persisted story blocks in a focused long-form layout.
      """
    Then CSS "#reader-title" contains environment variable "STORYBLOCK_UAT_NOVEL_TITLE"
    And CSS "#stat-han" has text "10,000"
    And CSS "#stat-zombies" has text "1,000"
    And CSS "#stat-cannons" has text "1,000"
    When I narrate in "en-US" for at least 8 seconds:
      """
      Counts and hashes come from the stored canonical revision, so the administrator can audit what was actually saved rather than trusting an authoring response.
      """
    And I click CSS "#console-tab"
    And I click CSS ".quick-requests button[data-path='/v1/openapi.yaml']"
    Then CSS "#response" eventually contains text "/agent/novels:"
    When I narrate in "en-US" for at least 8 seconds:
      """
      The secondary diagnostics view is GET-only. AI authors write through a separate validated skill and registration endpoint, while this browser remains a read-only review surface.
      """
    Then I finish the recorded demo
