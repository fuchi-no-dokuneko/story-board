# Source layout / 來源配置 / 源码布局

EN: Root applications are `server`, `client-cli`, `client-style-worker`, `client-llm-worker`, and `plugin`; shared Java libraries stay under `modules`. `skills/storyblock-author` is a compatibility link to `plugin`. Each application has its own local installation entry point. Shared tools live in `.local-tool`; acceptance-only dependencies live in `acceptance/.local-tool-app`.

繁體中文：根目錄應用為 `server`、`client-cli`、`client-style-worker`、`client-llm-worker` 及 `plugin`；共用 Java 程式庫位於 `modules`。`skills/storyblock-author` 為指向 `plugin` 的相容連結。各應用有本機安裝入口。共用工具位於 `.local-tool`，驗收專用依賴位於 `acceptance/.local-tool-app`。

简体中文：根目录应用为 `server`、`client-cli`、`client-style-worker`、`client-llm-worker` 及 `plugin`；共享 Java 库位于 `modules`。`skills/storyblock-author` 为指向 `plugin` 的兼容链接。各应用有本机安装入口。共享工具位于 `.local-tool`，验收专用依赖位于 `acceptance/.local-tool-app`。

EN: Authored files stay below 3000 bytes. Large fixed contracts, browser assets, and integration scenarios use ordered `*.parts/*.part` source sections. Local installation and `mvnw` assemble the original filename without changing its bytes. Edit sections directly, or edit the assembled file and save it with the command below. Generated files are ignored by Git. Markdown pages also stay below 200 lines and provide three languages.

繁體中文：撰寫檔案小於 3000 位元組。大型固定契約、瀏覽器資源與整合情境使用依序排列的 `*.parts/*.part` 來源區段。本機安裝與 `mvnw` 組合原檔名並保留位元組。可直接編輯區段，或編輯組合檔後以下列命令保存。Git 忽略生成檔；Markdown 也限制二百行並提供三語。

简体中文：编写文件小于 3000 字节。大型固定契约、浏览器资源与集成场景使用依序排列的 `*.parts/*.part` 源码区段。本机安装与 `mvnw` 组合原文件名并保留字节。可直接编辑区段，或编辑组合文件后以下列命令保存。Git 忽略生成文件；Markdown 也限制二百行并提供三语。

```bash
python3 scripts/split-source.py path/to/assembled-file
python3 scripts/assemble-sources.py
```
