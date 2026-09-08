# Run workflows / 執行流程 / 执行流程

EN: Run from the repository root against an isolated installed server. Set the HTTPS origin and existing Chromium executable. Prepare and register the fixture once; later runs reuse it. The browser selects its exact ID even when other novels exist.

繁體中文：在儲存庫根目錄對隔離的已安裝伺服器執行。指定 HTTPS 來源與既有 Chromium 執行檔。只準備及登錄一次範例，之後重用；其他作品存在時仍依精確 ID 選取。

简体中文：在仓库根目录对隔离的已安装服务器执行。指定 HTTPS 来源与现有 Chromium 执行文件。只准备及登记一次示例，之后复用；其他作品存在时仍按精确 ID 选择。

```bash
source scripts/build-environment.sh
export STORYBLOCK_BASE_URL=https://127.0.0.1:18443/
export CHROME_BINARY=/path/to/chromium
./acceptance/install-local.sh
node acceptance/prepare-fixture.mjs
node plugin/scripts/storyblock-author.mjs register --source .local/refactor-evidence/gui/manuscript.json --json
node acceptance/run-fixture-uat.mjs
```

EN: The image workflow registers a separate short novel, uploads the supplied PNG/JPEG, previews and commits an edit, and renders PDFs. Transfer verification needs a second isolated HTTPS server and preserves the original image/PDF bytes. These commands write only to the selected test servers and repository evidence directory.

繁體中文：圖片流程另登錄短篇作品，上傳指定 PNG/JPEG，預覽並提交編輯，再產生 PDF。轉移驗證需要第二個隔離 HTTPS 伺服器，並保留原始圖片／PDF 位元組。命令只寫入指定測試伺服器與儲存庫證據目錄。

简体中文：图片流程另登记短篇作品，上传指定 PNG/JPEG，预览并提交编辑，再生成 PDF。转移验证需要第二个隔离 HTTPS 服务器，并保留原始图片／PDF 字节。命令只写入指定测试服务器与仓库证据目录。

```bash
node acceptance/image-workflow.mjs path/to/image.png
node acceptance/transfer-workflow.mjs https://127.0.0.1:18444/
./scripts/test-lifecycle.sh
```
