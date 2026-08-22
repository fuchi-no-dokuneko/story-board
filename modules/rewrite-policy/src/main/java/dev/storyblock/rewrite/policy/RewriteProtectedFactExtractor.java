package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.validator.EvidenceSpans;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
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
    private static final List<String> PRESENCE = List.of(
            "進入", "进入", "走進", "走进", "離開", "离开", "退出", "離場",
            "enter", "entered", "exit", "exited", "left"
    );
    private static final Set<String> EVIDENCE_FIELDS = Set.of(
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

        addLexicon(facts, ProtectedFactKind.NAME, normalized, lexicon.names());
        addLexicon(facts, ProtectedFactKind.PLACE, normalized, lexicon.places());
        addMatches(facts, ProtectedFactKind.NUMBER, NUMBER.matcher(normalized));
        addMarkers(facts, ProtectedFactKind.NEGATION, normalized, NEGATIONS);
        addMarkers(facts, ProtectedFactKind.CAUSALITY, normalized, CAUSALITY);
        addMarkers(facts, ProtectedFactKind.PRESENCE, normalized, PRESENCE);

        Map<String, Object> metadata = block.metadata().fields();
        addSpeakers(facts, metadata.get("speech"));
        addPresenceEvents(facts, metadata.get("presence_events"));
        addMetadataFacts(facts, metadata.get("actions"), "actions");
        inspectEvidence(metadata, normalized, facts, manual);

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

    private static void addLexicon(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            String text,
            List<String> values
    ) {
        for (String value : values) {
            int count = occurrences(text, value, false);
            if (count > 0) {
                add(facts, kind, value, count);
            }
        }
    }

    private static void addMatches(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            Matcher matcher
    ) {
        while (matcher.find()) {
            add(facts, kind, matcher.group(), 1);
        }
    }

    private static void addMarkers(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            String text,
            List<String> markers
    ) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            boolean word = marker.chars().allMatch(value -> value < 128);
            int count = occurrences(lower, marker, word);
            if (count > 0) {
                add(facts, kind, marker, count);
            }
        }
    }

    private static int occurrences(String text, String value, boolean wordBoundary) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(value, from)) >= 0) {
            int end = from + value.length();
            if (!wordBoundary || (boundary(text, from - 1) && boundary(text, end))) {
                count++;
            }
            from = end;
        }
        return count;
    }

    private static boolean boundary(String value, int index) {
        return index < 0 || index >= value.length()
                || !Character.isLetterOrDigit(value.codePointAt(index));
    }

    private static void addSpeakers(Map<FactKey, Integer> facts, Object value) {
        if (!(value instanceof Map<?, ?> speech)) {
            return;
        }
        addStringValues(facts, ProtectedFactKind.SPEAKER, speech.get(
                "direct_speaker_id"
        ));
        addStringValues(facts, ProtectedFactKind.SPEAKER, speech.get(
                "direct_speaker_ids"
        ));
        addStringValues(facts, ProtectedFactKind.SPEAKER, speech.get("speaker_id"));
        if (speech.get("turns") instanceof List<?> turns) {
            for (Object valueEntry : turns) {
                if (valueEntry instanceof Map<?, ?> turn
                        && "direct".equals(turn.get("channel"))) {
                    addStringValues(
                            facts, ProtectedFactKind.SPEAKER, turn.get("speaker_id")
                    );
                }
            }
        }
    }

    private static void addPresenceEvents(Map<FactKey, Integer> facts, Object value) {
        if (!(value instanceof List<?> events)) {
            return;
        }
        for (Object entry : events) {
            if (!(entry instanceof Map<?, ?> event)) {
                continue;
            }
            Map<String, Object> identity = new LinkedHashMap<>();
            identity.put("character_id", event.get("character_id"));
            identity.put("type", event.get("type"));
            add(facts, ProtectedFactKind.PRESENCE, CanonicalJson.string(identity), 1);
        }
    }

    private static void addMetadataFacts(
            Map<FactKey, Integer> facts,
            Object value,
            String field
    ) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                add(facts, ProtectedFactKind.HIGH_RISK_METADATA,
                        field + ":" + CanonicalJson.string(entry), 1);
            }
        } else {
            add(facts, ProtectedFactKind.HIGH_RISK_METADATA,
                    field + ":" + CanonicalJson.string(value), 1);
        }
    }

    private static void inspectEvidence(
            Object value,
            String text,
            Map<FactKey, Integer> facts,
            Set<String> manual
    ) {
        if (value instanceof Map<?, ?> map) {
            if (map.keySet().containsAll(EVIDENCE_FIELDS)) {
                Object quoteHash = map.get("quote_hash");
                if (EvidenceSpans.matches(text, map) && quoteHash instanceof String hash) {
                    add(facts, ProtectedFactKind.EVIDENCE, hash, 1);
                } else {
                    manual.add("stale_evidence");
                }
                return;
            }
            map.values().forEach(entry -> inspectEvidence(entry, text, facts, manual));
        } else if (value instanceof List<?> list) {
            list.forEach(entry -> inspectEvidence(entry, text, facts, manual));
        }
    }

    private static void addStringValues(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            Object value
    ) {
        if (value instanceof String text && !text.isBlank()) {
            add(facts, kind, text, 1);
        } else if (value instanceof Collection<?> values) {
            values.forEach(entry -> addStringValues(facts, kind, entry));
        }
    }

    private static void add(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            String value,
            int count
    ) {
        String hash = CanonicalJson.hash(Map.of(
                "kind", kind.canonicalName(), "value", value
        ));
        facts.merge(new FactKey(kind, hash), count, Integer::sum);
    }

    private record FactKey(ProtectedFactKind kind, String valueHash) {
    }
}
