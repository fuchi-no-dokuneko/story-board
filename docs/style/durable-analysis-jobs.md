# Durable analysis

EN: Analysis snapshots bind immutable revisions and profiles. Workers claim fenced leases over HTTPS, compute outside transactions, and submit one canonical result. Expired leases can be reclaimed; stale attempts cannot overwrite results. Trace retention and compact durable summaries remain unchanged.

繁體中文：分析快照綁定不可變修訂與設定。工作程式透過 HTTPS 領取有隔離識別的租約，在交易外計算，再提交一份標準結果。過期租約可重新領取，舊嘗試不能覆寫結果。追蹤保留期限與精簡永久摘要不變。

简体中文：分析快照绑定不可变修订与设置。工作程序通过 HTTPS 领取有隔离标识的租约，在事务外计算，再提交一份标准结果。过期租约可重新领取，旧尝试不能覆盖结果。跟踪保留期限与精简永久摘要不变。

[Detailed notes / 詳細筆記 / 详细笔记](durable-analysis-jobs.notes.txt)
