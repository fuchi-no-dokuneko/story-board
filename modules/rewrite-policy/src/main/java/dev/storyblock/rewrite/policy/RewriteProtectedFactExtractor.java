package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.style.StyleMaskingLexicon;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class RewriteProtectedFactExtractor {
    private static final Pattern NUMBER = Pattern.compile(
            "(?:\\p{N}+(?:[.,:/-]\\p{N}+)*)|[〇零一二三四五六七八九十百千万萬亿億两兩壹貳叁參肆伍陆陸柒捌玖拾佰仟]+"
    );
    private static final List<String> NEGATIONS = List.of(
            "不", "沒有", "没有", "未", "無", "无", "不是", "不能",
            "never", "no", "not", "without"
    );
    private static final List<String> CAUSALITY = List.of(
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
        java.util.Objects.requireNonNull(block, "block");
        java.util.Objects.requireNonNull(text, "text");
        java.util.Objects.requireNonNull(lexicon, "lexicon");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        Map<FactKey, Integer> facts = new LinkedHashMap<>();
        TreeSet<String> manual = new TreeSet<>();

        RewriteProtectedFactExtractorAddLexicon.addLexicon(facts, ProtectedFactKind.NAME, normalized, lexicon.names());
        RewriteProtectedFactExtractorAddLexicon.addLexicon(facts, ProtectedFactKind.PLACE, normalized, lexicon.places());
        RewriteProtectedFactExtractorAddMatches.addMatches(facts, ProtectedFactKind.NUMBER, NUMBER.matcher(normalized));
        RewriteProtectedFactExtractorAddMarkers.addMarkers(facts, ProtectedFactKind.NEGATION, normalized, NEGATIONS);
        RewriteProtectedFactExtractorAddMarkers.addMarkers(facts, ProtectedFactKind.CAUSALITY, normalized, CAUSALITY);
        RewriteProtectedFactExtractorAddMarkers.addMarkers(facts, ProtectedFactKind.PRESENCE, normalized, PRESENCE);

        Map<String, Object> metadata = block.metadata().fields();
        RewriteProtectedFactExtractorAddSpeakers.addSpeakers(facts, metadata.get("speech"));
        RewriteProtectedFactExtractorAddPresenceEvents.addPresenceEvents(facts, metadata.get("presence_events"));
        RewriteProtectedFactExtractorAddMetadataFacts.addMetadataFacts(facts, metadata.get("actions"), "actions");
        RewriteProtectedFactExtractorInspectEvidence.inspectEvidence(metadata, normalized, facts, manual);

        List<RewriteProtectedFact> ordered = facts.entrySet().stream()
                .map(entry -> new RewriteProtectedFact(
                        entry.getKey().kind(), entry.getKey().valueHash(), entry.getValue()
                ))
                .sorted(Comparator
                        .comparing((RewriteProtectedFact fact) -> fact.kind().ordinal())
                        .thenComparing(RewriteProtectedFact::valueHash))
                .toList();
        return new RewriteProtectedFactSnapshot(
                block.id(), ordered, List.copyOf(manual)
        );
    }

    record FactKey(ProtectedFactKind kind, String valueHash) {
    }
}
