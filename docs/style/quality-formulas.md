# Formula contract / 計算契約 / 计算契约

English: Jieba SEARCH emits exclusive words. Whitespace and punctuation are excluded from word counts; punctuation remains available to skeletons. Tokenize once; measure both whole text and windows. Windows extend to complete sentences until at least 256 words; starts advance by complete sentences covering at least 128 words. A long sentence may exceed the window target. The final window ends at the final sentence.

繁體：SEARCH 詞段互斥；詞數不含空白與標點，骨架保留標點。每次只分詞一次。全文與視窗各自計分；視窗收取完整句直到至少 256 詞，起點每次移動至少 128 詞的完整句，末窗止於末句。

简体：SEARCH 词段互斥；词数不含空白与标点，骨架保留标点。每次只分词一次。全文与窗口分别计分；窗口收取完整句直到至少 256 词，起点每次移动至少 128 词的完整句，末窗止于末句。

| Metric / 指標 / 指标 | Definition / 定義 / 定义 |
| --- | --- |
| Repeated phrase / 重複片語 / 重复短语 | Sentence-local 4–8 words; cross-sentence repeats, first occurrence excluded, later token coverage union / all words. |
| Opening / 句首 | First 3 words; names become one category. Σ c(c−1) / S(S−1); S ≥ 5. |
| Skeleton / 骨架 | Function words and punctuation retained; other runs become X. At least 2 markers; repeats after first / valid sentences; S ≥ 5. |
| Rhythm / 節奏 / 节奏 | 1 − H(next class given previous) / log₂3; at least 10 transitions. |
| Formulaic / 套語 / 套语 | Positive ln((pA+ε)/(pH+ε)), ε=.001; ≥2 AI sources support a phrase. Each token receives maximum covering weight, averaged over all words. |

English: Each reference source contributes equal weight; phrase rates count windows containing a phrase once. Human sentence-length tertiles establish fixed short/medium/long boundaries. Human reference percentiles use weighted midranks (equal sources, equal valid windows within each source), 0–100. Reference scores describe the supplied calibration sample, not an independent validation set. Whole-text percentiles compare against reference windows. JSD uses base-2 word frequencies against equal-source human frequencies; MATTR uses 50-word windows. Evidence is capped at 20 excerpts per metric, without changing scores. Exact parameters and wordlists appear in `contract`.

繁體：來源等權，片語在每窗最多計一次。真人句長三分位定固定界線；分位數採等來源加權中位排名。全文分數亦比較參考視窗。JSD 以 2 為底；MATTR 為 50 詞。每指標最多 20 段證據，不影響分數；參數與詞表見契約。

简体：来源等权，短语每窗最多计一次。真人句长三分位确定固定边界；分位数采用等来源加权中位排名。全文分数也比较参考窗口。JSD 以 2 为底；MATTR 为 50 词。每指标最多 20 段证据，不影响分数；参数和词表见契约。
