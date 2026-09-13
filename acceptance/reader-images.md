# Reader images / 閱讀圖片 / 阅读图片

EN: Run from the repository root with Chromium and Poppler (`pdfimages`) installed. Use an isolated local HTTPS database. Supply one JPEG and one PNG; the fixture contains ordinary Chinese prose and labels both format samples. Preparation prints the novel ID, surrounding blocks and preview; inspect them before committing within five minutes.

繁體中文：在儲存庫根目錄執行，需要 Chromium、Poppler 及隔離的本機 HTTPS 資料庫。提供 JPEG、PNG 各一張；範例使用普通中文並標示格式樣本。準備命令印出小說 ID、周邊段落與預覽，檢查後於五分鐘內提交。

简体中文：在仓库根目录执行，需要 Chromium、Poppler 及隔离的本机 HTTPS 数据库。提供 JPEG、PNG 各一张；示例使用普通中文并标明格式样本。准备命令打印小说 ID、周边段落与预览，检查后于五分钟内提交。

```bash
python3 scripts/assemble-sources.py
node acceptance/prepare-reader-images.mjs https://127.0.0.1:18445 sample.jpg sample.png .local/image-fixture
# Substitute the printed novel ID / 代入輸出 ID / 代入输出 ID
node plugin/scripts/storyblock-author.mjs commit --novel-id nov_Ab123 --file .local/image-fixture/edit.json --base-url https://127.0.0.1:18445 --json
node acceptance/reader-images.cjs https://127.0.0.1:18445 nov_Ab123 .local/image-browser
```

EN: Reuse the committed novel for subsequent browser runs. Each run creates a fresh output directory. The runner checks decoded images, canonical captions/order, 390-pixel layout, a simulated 404/retry, URL cleanup, PDF page/image headers and UTF-8 text downloads. PDF pages are rasters; inspect their illustrations visually. For a local server with trusted access disabled, append the owner-token file path; this also checks authenticated image fetches, anonymous 401 and logout cleanup.

繁體中文：後續重用已提交小說，每次產生新證據目錄。測試真實圖片解碼、圖說順序、390 像素版面、模擬 404／重試、資源釋放及 PDF／TXT 下載。PDF 為頁面影像，插圖另需目視確認。停用信任存取時附加金鑰檔路徑，可驗證登入取圖、匿名 401 與登出清理。

简体中文：后续复用已提交小说，每次生成新证据目录。测试真实图片解码、图注顺序、390 像素版面、模拟 404／重试、资源释放及 PDF／TXT 下载。PDF 为页面图像，插图另需目视确认。禁用信任访问时追加密钥文件路径，可验证登录取图、匿名 401 与退出清理。
