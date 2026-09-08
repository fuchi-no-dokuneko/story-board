# LLM worker

EN: The worker reads one bounded immutable input, calls one HTTPS model endpoint, and emits one text proposal. It has no database or canonical commit capability. Model credentials are optional; local self-signed TLS is accepted. Existing size limits, source bindings, and tool-free protocol remain intact.

繁體中文：工作程式讀取一份有界不可變輸入，呼叫單一 HTTPS 模型端點，輸出一份文字提案。它沒有資料庫或正式提交能力。模型密鑰為選填，接受本機自簽 TLS。既有大小限制、來源綁定及無工具協定均保留。

简体中文：工作程序读取一份有界不可变输入，调用单一 HTTPS 模型端点，输出一份文字提案。它没有数据库或正式提交能力。模型密钥为选填，接受本机自签 TLS。既有大小限制、来源绑定及无工具协议均保留。

[Detailed notes / 詳細筆記 / 详细笔记](sandboxed-llm-worker.notes.txt)
