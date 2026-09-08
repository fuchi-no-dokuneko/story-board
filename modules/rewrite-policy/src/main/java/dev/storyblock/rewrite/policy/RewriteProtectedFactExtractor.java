package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.style.StyleMaskingLexicon;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class RewriteProtectedFactExtractor {
    static final Pattern NUMBER = Pattern.compile(
            "(?:\\p{N}+(?:[.,:/-]\\p{N}+)*)|[〇零一二三四五六七八九十百千万萬亿億两兩壹貳叁參肆伍陆陸柒捌玖拾佰仟]+"
    );
    static final List<String> NEGATIONS = List.of(
            "不", "沒有", "没有", "未", "無", "无", "不是", "不能",
            "never", "no", "not", "without"
    );
    static final List<String> CAUSALITY = List.of(
            "因為", "因为", "所以", "因此", "由於", "由于", "導致", "导致",
            "because", "caused", "therefore"
    );
    static final List<String> PRESENCE = List.of(
            "進入", "进入", "走進", "走进", "離開", "离开", "退出", "離場",
            "enter", "entered", "exit", "exited", "left"
    );
    static final Set<String> EVIDENCE_FIELDS = Set.of(
            "start_grapheme", "end_grapheme", "quote", "quote_hash"
    );

    public RewriteProtectedFactSnapshot snapshot(
            NarrativeBlock block,
            String text,
            StyleMaskingLexicon lexicon
    ) {
        return RewriteProtectedFactExtractorSnapshotAction.snapshot(this, block, text, lexicon);
    }

    record FactKey(ProtectedFactKind kind, String valueHash) {
    }
}
