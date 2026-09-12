# Preview and commit

EN: Preview does not persist a revision. Commit resubmits the exact operation and candidate identity, validates the current head, and uses the existing compare-and-swap transaction. Identical idempotent retries return the original result; changed payloads conflict. The retained design note describes the earlier token proposal.

繁體中文：預覽不會儲存修訂。提交重送相同操作與候選身分，驗證目前版本頭，並使用既有比較交換交易。同鍵相同內容重試回傳原結果，內容變更則衝突。保留筆記記錄早期權杖設計提案。

简体中文：预览不会保存修订。提交重发相同操作与候选身份，验证当前版本头，并使用既有比较交换事务。同键相同内容重试返回原结果，内容变更则冲突。保留笔记记录早期令牌设计提案。

[Detailed notes / 詳細筆記 / 详细笔记](0007-stateless-preview-commit.notes.txt)
