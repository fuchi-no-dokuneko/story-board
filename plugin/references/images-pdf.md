# Images and PDF / 圖片與 PDF / 图片与 PDF

EN: Upload immutable PNG/JPEG bytes first. Use the returned `block_image` descriptor as `BlockDraft.image`; text becomes its caption. Uploads allow 1–1,500,000 bytes, dimensions up to 8192×8192, and at most 40 million pixels. Image blocks can be inserted, replaced, moved, or deleted; text-only split, merge, and extend are rejected.

繁體中文：先上傳不可變 PNG/JPEG 位元組，再將回傳 `block_image` 用作 `BlockDraft.image`，文字為圖說。允許一至一百五十萬位元組、最多 8192×8192 尺寸及四千萬像素。圖片區塊可插入、取代、移動、刪除；文字專用拆分、合併、延伸會被拒絕。

简体中文：先上传不可变 PNG/JPEG 字节，再将返回 `block_image` 用作 `BlockDraft.image`，文字为图注。允许一至一百五十万字节、最多 8192×8192 尺寸及四千万像素。图片区块可插入、替换、移动、删除；文字专用拆分、合并、延伸会被拒绝。

```bash
node scripts/storyblock-author.mjs upload-image --novel-id nov_Ab123 --file portrait.png --alt-text 'Character portrait' --json
node scripts/storyblock-author.mjs render-pdf --novel-id nov_Ab123 --file pdf-request.json --output novel.pdf --json
```

EN: The library displays saved image blocks with captions and mobile scaling. Failed loads offer retry; protected mode uses the shared owner token.

繁體中文：書庫顯示已儲存圖片、圖說及手機縮放；載入失敗可重試，受保護模式使用共用擁有者金鑰。

简体中文：书库显示已保存图片、图注及手机缩放；加载失败可重试，受保护模式使用共享所有者密钥。

EN: Validate character references before later illustration: the characters that need illustrations, one initial image and 2–6 variants each, plain backgrounds, and stable identity locks. PDF rendering binds the immutable revision and outputs deterministic A4 text, images, and captions. Image history transfers through canonical packages. Binary outputs are private and need `--force` to overwrite.

繁體中文：後續繪圖前驗證角色參照：需要繪圖的角色，各一張初始圖與二至六張變體，純色背景及穩定身分鎖。PDF 綁定不可變修訂，輸出確定性 A4 文字、圖片及圖說。含圖片歷史以標準封裝轉移。二進位輸出限私人讀取，覆寫需 `--force`。

简体中文：后续绘图前验证角色引用：需要绘图的角色，各一张初始图与二至六张变体，纯色背景及稳定身份锁。PDF 绑定不可变修订，输出确定性 A4 文字、图片及图注。含图片历史以标准封装转移。二进制输出限私人读取，覆盖需 `--force`。
