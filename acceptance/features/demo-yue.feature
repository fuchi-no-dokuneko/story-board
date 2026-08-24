@demo @recording @cantonese @web
Feature: StoryBlock Console 廣東話產品介紹
  呢段錄影會介紹產品同示範主要請求流程，而且唔會改動小說資料。

  Scenario: 介紹主控台並安全檢視 API
    Given I begin a recorded demo
    And I open the web application at path "/"
    When I narrate in "yue-Hant-HK" for at least 10 seconds:
      """
      StoryBlock 係一個本機優先嘅長篇故事編輯引擎。每次修改都係不可變，而且連住驗證證據。呢個簡潔主控台，俾操作員直接睇到 API 實際回應。
      """
    Then CSS "#status-text" eventually contains text "API online"
    When I narrate in "yue-Hant-HK" for at least 7 seconds:
      """
      頂部狀態會話你知 API 係咪在線。我哋首先撳 Health，確認服務正常，再喺下面核對 HTTP 狀態同 JSON。
      """
    And I click CSS "nav button[data-path='/actuator/health']"
    Then CSS "#response-status" eventually contains text "200"
    And CSS "#response" eventually contains text "\"status\": \"UP\""
    When I narrate in "yue-Hant-HK" for at least 9 seconds:
      """
      JSON 會自動排版，方便逐項睇。OpenAPI 快捷掣就會顯示版本化 YAML 合約，想了解支援邊啲路徑，可以由呢度開始。
      """
    And I click CSS "nav button[data-path='/v1/openapi.yaml']"
    Then CSS "#response" eventually contains text "openapi:"
    And CSS "#response" contains text "/novels:"
    When I narrate in "yue-Hant-HK" for at least 10 seconds:
      """
      自訂請求可以揀 GET、POST 或 DELETE，再填路徑同 JSON。只有受保護路徑先需要 bearer token，而密碼欄會遮住內容，回應亦唔應該洩漏憑證。
      """
    And I choose value "POST" in CSS "#method"
    And I replace CSS "#path" with "/v1/novels"
    And I replace CSS "#body" with "{}"
    And I click CSS "#send"
    Then CSS "#response-status" eventually contains text "401"
    And CSS "#response" eventually contains text "AUTHENTICATION_REQUIRED"
    When I narrate in "yue-Hant-HK" for at least 8 seconds:
      """
      呢個未授權回應係預期結果，證明系統預設拒絕未認證請求。操作員可以睇狀態同錯誤碼，過程唔會改動任何小說內容。
      """
    Then I finish the recorded demo
