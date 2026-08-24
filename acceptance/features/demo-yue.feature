@demo @recording @cantonese @web
Feature: StoryBlock 小說庫廣東話介紹
  錄影示範本機唯讀審閱流程，全程不會修改小說資料。

  Scenario: 閱讀代理創作小說並檢視合約
    Given I begin a recorded demo
    And I open the web application at path "/"
    When I narrate in "yue-Hant-HK" for at least 8 seconds:
      """
      StoryBlock 係本機優先嘅不可變小說版本庫。呢個內聯網管理畫面可以睇晒已儲存小說，而且冇編輯或者刪除掣。
      """
    Then CSS "#status-text" eventually contains text "Service online"
    And CSS "#reader-content" is visible
    When I narrate in "yue-Hant-HK" for at least 9 seconds:
      """
      左邊目錄會開啟目前版本，閱讀器清楚列出書名、五位主角、章節導覽，同埋資料庫實際保存嘅故事段落。
      """
    Then CSS "#reader-title" contains environment variable "STORYBLOCK_UAT_NOVEL_TITLE"
    And CSS "#stat-han" has text "10,000"
    And CSS "#stat-zombies" has text "1,000"
    And CSS "#stat-cannons" has text "1,000"
    When I narrate in "yue-Hant-HK" for at least 8 seconds:
      """
      字數、殭屍總數、炸藥炮總數同雜湊都來自持久化版本，所以管理員驗證緊嘅係真正落咗資料庫嘅內容。
      """
    And I click CSS "#console-tab"
    And I click CSS ".quick-requests button[data-path='/v1/openapi.yaml']"
    Then CSS "#response" eventually contains text "/agent/novels:"
    When I narrate in "yue-Hant-HK" for at least 8 seconds:
      """
      診斷畫面只准讀取。人工智能作者會經獨立技能同註冊端點寫入，瀏覽器管理頁就保持唯讀。
      """
    Then I finish the recorded demo
