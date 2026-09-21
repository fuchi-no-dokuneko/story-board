# Chinese segmentation / 中文分詞 / 中文分词

English: Internal adaptation of [huaban/jieba-analysis](https://github.com/huaban/jieba-analysis), Apache-2.0. Trie, DAG, dynamic programming and HMM/Viterbi follow its SEARCH path. Local changes replace mutable globals with immutable packed dictionaries, use Unicode code points, retain original UTF-16 offsets and isolate proper-name overrides per analysis. INDEX overlap is not emitted.

繁體：內部改編自上述 Java 專案，採 Apache-2.0。保留 Trie、DAG、動態規劃與 HMM/Viterbi 的 SEARCH 路徑；詞典改為唯讀快照，以碼點處理增補漢字，保留原文 UTF-16 位置，專名覆寫限於單次分析。

简体：内部改编自上述 Java 项目，采用 Apache-2.0。保留 Trie、DAG、动态规划与 HMM/Viterbi 的 SEARCH 路径；词典改为只读快照，按码点处理增补汉字，保留原文 UTF-16 位置，专名覆盖限于单次分析。

| Asset / 資產 / 资产 | Source / 來源 / 来源 | License / 授權 / 授权 |
| --- | --- | --- |
| `dict.txt.big.gz` | [fxsjy/jieba extra_dict/dict.txt.big](https://github.com/fxsjy/jieba/blob/master/extra_dict/dict.txt.big) | MIT, `licenses/jieba-MIT.txt` |
| `prob_emit.txt.gz` | [huaban HMM emissions](https://github.com/huaban/jieba-analysis/blob/master/src/main/resources/prob_emit.txt) | Apache-2.0, `licenses/Apache-2.0.txt` |
| Java algorithm / 演算法 / 算法 | [JiebaSegmenter](https://github.com/huaban/jieba-analysis/blob/master/src/main/java/com/huaban/analysis/jieba/JiebaSegmenter.java), [FinalSeg](https://github.com/huaban/jieba-analysis/blob/master/src/main/java/com/huaban/analysis/jieba/viterbi/FinalSeg.java) | Apache-2.0 |

Retrieved / 取得 / 获取：2026-09-21. GZIP preserves the complete upstream text; builds use only bundled resources. Full licenses are included in the JAR. 建置只讀取內附資產，JAR 附完整授權。构建只读取内附资产，JAR 附完整授权。
