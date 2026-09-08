#!/usr/bin/env node
import { readdir, readFile } from "node:fs/promises";
import { writePage, writeIndex, contractLink } from "./catalog-pages.mjs";

const references = new URL("../references/", import.meta.url);
const manifest = JSON.parse(await readFile(new URL("endpoints.json", references), "utf8"));
await writeIndex(references, "endpoints", manifest.endpoints.map(item => item.id),
  `${manifest.endpoints.length} code-verified programmatic routes.\n` +
  `${manifest.endpoints.length} 個由程式確認的程式化路由。\n` +
  `${manifest.endpoints.length} 个由程序确认的程序化路由。`);
for (const endpoint of manifest.endpoints) {
  await writePage(new URL(`endpoints/${endpoint.id}.md`, references),
    `# ${endpoint.id}\n\n${contractLink("../endpoints.json")}\n` +
    `| Field / 欄位 / 字段 | Value / 值 / 值 |\n| --- | --- |\n` +
    `| Method / 方法 / 方法 | ${endpoint.method} |\n` +
    `| Path / 路徑 / 路径 | \`${endpoint.path}\` |\n` +
    `| Request / 請求 / 请求 | ${endpoint.requestBody?.dto ?? "—"} |\n` +
    `| Response / 回應 / 响应 | ${endpoint.responses.map(r => `${r.status}: ${r.dto ?? "—"}`).join("; ")} |\n` +
    `| Scopes / 範圍 / 范围 | ${endpoint.scopes.join(", ") || "—"} |\n\n` +
    `The default local server trusts reachable clients; credentials are optional.\n` +
    `預設本機伺服器信任可連線裝置；密鑰為選填。\n` +
    `默认本机服务器信任可连接设备；密钥为选填。\n`);
}
const files = (await readdir(new URL("dtos/", references))).filter(name => name.endsWith(".schema.json")).sort();
await writeIndex(references, "dto-pages", files.map(name => name.replace(".schema.json", "")),
  `${files.length} standalone JSON Schema documents.\n${files.length} 份獨立 JSON Schema 文件。\n${files.length} 份独立 JSON Schema 文件。`);
await writePage(new URL("dtos.md", references),
  `# DTO catalog / DTO 目錄 / DTO 目录\n\n${files.length} standalone JSON Schema documents.\n` +
  `${files.length} 份獨立 JSON Schema 文件。\n${files.length} 份独立 JSON Schema 文件。\n\n` +
  `[Browse / 瀏覽 / 浏览](dto-pages.md)\n`);
for (const file of files) {
  const name = file.replace(".schema.json", "");
  await writePage(new URL(`dto-pages/${name}.md`, references),
    `# ${name}\n\n${contractLink(`../dtos/${file}`)}\n` +
    `The schema includes field constraints, examples, and source provenance. Validate with the CLI before sending a request.\n` +
    `綱要包含欄位限制、範例及來源。送出請求前先使用 CLI 驗證。\n` +
    `纲要包含字段限制、示例及来源。发送请求前先使用 CLI 验证。\n`);
}
