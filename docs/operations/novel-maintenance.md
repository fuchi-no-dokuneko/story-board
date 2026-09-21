# Novel maintenance / 小說維護 / 小说维护

English: Build once with `./client-cli/install-local.sh`. Run scripts from the repository; `--database` selects an existing SQLite file (default `server/data/storyblock.db`). Keep the API stopped during deletion. Preview is the default; only `--apply` deletes. Selected novel records are deleted in one transaction. References from retained data cause a rollback.

繁體：先執行 `./client-cli/install-local.sh` 建置。腳本的 `--database` 指定既有 SQLite，預設為 `server/data/storyblock.db`。刪除時先停止 API。預設只預覽，加入 `--apply` 才執行；所選小說在同一交易中刪除。若保留資料仍有引用，交易會回滾。

简体：先执行 `./client-cli/install-local.sh` 构建。脚本的 `--database` 指定已有 SQLite，默认为 `server/data/storyblock.db`。删除时先停止 API。默认只预览，加入 `--apply` 才执行；所选小说在同一事务中删除。如保留数据仍有引用，事务会回滚。

```sh
./server/delete-novels.sh --list
./server/delete-novels.sh --novel nov_XXXXXXXX
./server/delete-novels.sh --novel nov_XXXXXXXX --apply
./server/export-novels.sh --all --format both
./server/export-novels.sh --novel nov_XXXXXXXX --format html --output fan-out/books
```

English: IDs above are placeholders; copy real IDs from the list. Repeat `--novel` for a selection, or use `--all`. Exports capture selected head revisions and produce ID-named files plus `index.html`. HTML embeds images for offline reading; PDFs embed the existing Chinese fonts. Output stays inside the repository, default `fan-out/novels`. An unknown ID or rendering failure exits nonzero; already completed files remain available.

繁體：上例 ID 是佔位符，請從清單複製實際 ID。可重複 `--novel` 或用 `--all`。匯出所選最新修訂，產生 ID 命名檔及索引；HTML 內嵌圖片，PDF 使用既有中文字型。輸出限於儲存庫，預設 `fan-out/novels`。失敗時傳回非零狀態，已完成檔案仍保留。

简体：上例 ID 是占位符，请从列表复制实际 ID。可重复 `--novel` 或用 `--all`。导出所选最新修订，生成 ID 命名文件及索引；HTML 内嵌图片，PDF 使用已有中文字体。输出限于仓库，默认 `fan-out/novels`。失败时返回非零状态，已完成文件仍保留。
