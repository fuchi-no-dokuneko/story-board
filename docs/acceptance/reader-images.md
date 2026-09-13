# Image reader verification / 圖片閱讀驗收 / 图片阅读验收

EN: Local installed build, 2026-09-13. The previous UI rendered image captions as plain paragraphs. It now loads authenticated PNG/JPEG artifacts, renders images and captions in canonical order, scales them to the viewport and offers retry after failure. Refresh, selection and credential clearing cancel pending loads and release object URLs.

繁體中文：2026-09-13 本機安裝版驗收。舊介面只有圖說；現在讀取受保護的 PNG／JPEG，依區塊順序顯示圖片與圖說，配合螢幕縮放並提供失敗重試。重新整理、選取與清除憑證會取消載入並釋放資源。

简体中文：2026-09-13 本机安装版验收。旧界面只有图注；现在读取受保护的 PNG／JPEG，按区块顺序显示图片与图注，随屏幕缩放并提供失败重试。刷新、选择与清除凭证会取消加载并释放资源。

EN: One novel, `nov_9Dqqj`, revision `rev_cP9et`: 547 Han, two chapters/scenes, 16 blocks, one JPEG and one PNG. Before the fix, the browser assertion failed with 0 images instead of 2. After reinstall/restart, real Chromium passed desktop/mobile, both access modes, simulated 404/retry, credential cleanup and fresh PDF/TXT downloads. Both downloaded image hashes matched their uploads. Five-page PDFs were byte-identical across access modes; illustration pages 3–4 and browser screenshots were visually inspected.

繁體中文：同一本新小說含 547 漢字、兩章／場景、16 區塊及兩種格式圖片。修正前實際圖片數為零；重裝重啟後，真實瀏覽器通過桌面／手機、兩種登入模式、模擬故障重試與下載。圖片雜湊吻合；五頁 PDF 位元組一致，已目視插圖頁與截圖。

简体中文：同一本新小说含 547 汉字、两章／场景、16 区块及两种格式图片。修正前实际图片数为零；重装重启后，真实浏览器通过桌面／手机、两种登录模式、模拟故障重试与下载。图片哈希吻合；五页 PDF 字节一致，已目视插图页与截图。

EN: Passed 8 browser-asset Node tests, 4 targeted Java HTTP/architecture tests and existing GUI UAT (9 scenarios/81 steps). Evidence and isolated database: `.local/reader-images-local/` and its `storyblock.db`. [Reproduce](../../acceptance/reader-images.md).

繁體中文：介面 Node 8 項、Java 相關 HTTP／架構 4 項及既有 GUI 9 情境／81 步驟通過。上述目錄保存證據與隔離資料庫。

简体中文：界面 Node 8 项、Java 相关 HTTP／架构 4 项及已有 GUI 9 场景／81 步骤通过。上述目录保存证据与隔离数据库。
