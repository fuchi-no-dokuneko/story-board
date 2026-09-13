# Style references / 風格參考 / 风格参考

EN: Register every style in `server/config/content-config/styles.yaml`. Put UTF-8 TXT files in `server/style-references/<style>/`, or use any absolute source directories, including paths outside the repository and paths with spaces. Relative sources resolve against the YAML directory. The YAML itself must remain inside the repository, including symbolic links. No raw-text upload API is required; copy reference files onto the server.

繁體中文：全部風格登記於 `server/config/content-config/styles.yaml`。UTF-8 TXT 可放 `server/style-references/<style>/` 或多個絕對目錄，允許 repo 外路徑與空格。相對路徑以 YAML 目錄為基準；YAML 本身及連結目標須留在 repo。參考文字直接複製到伺服器，本批次沒有原文上傳 API。

简体中文：全部风格登记于 `server/config/content-config/styles.yaml`。UTF-8 TXT 可放 `server/style-references/<style>/` 或多个绝对目录，允许 repo 外路径与空格。相对路径以 YAML 目录为基准；YAML 本身及链接目标须留在 repo。参考文字直接复制到服务器，本批次没有原文上传 API。

```yaml
schema_version: 1
styles:
  - id: lyrical
    name: 抒情敘事
    description: 節奏舒緩，偏重環境與內心描寫
    language: zh-Hant
    sources_dir:
      - ../../style-references/lyrical
      - /srv/reference novels/lyrical
```

EN: Startup and each 60-second scan inspect YAML timestamps and TXT inventory, timestamps and sizes recursively. Changed references rebuild in the background. `GET /v1/style/` shows readiness/errors. Comparisons use ready benchmarks; source failures do not silently use stale data. Sample texts demonstrate setup and are too small for reliable literary claims. The GUI compares five raw distances and links `/style-math.html`.

繁體中文：啟動及每 60 秒遞迴檢查 YAML 時間、TXT 清單、時間與大小，變動後背景重算。`GET /v1/style/` 顯示狀態與錯誤，比較僅使用可用 benchmark；來源失效不會偷偷沿用舊資料。範例文字僅示範設定，不足以支持文學風格結論。GUI 顯示五項距離並連結公式頁。

简体中文：启动及每 60 秒递归检查 YAML 时间、TXT 清单、时间与大小，变化后后台重算。`GET /v1/style/` 显示状态与错误，比较仅使用可用 benchmark；来源失效不会偷偷沿用旧资料。示例文字仅示范配置，不足以支持文学风格结论。GUI 显示五项距离并链接公式页。
