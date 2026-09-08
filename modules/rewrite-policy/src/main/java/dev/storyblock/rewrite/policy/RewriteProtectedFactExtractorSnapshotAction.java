package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.style.StyleMaskingLexicon;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorSnapshotAction {
    static RewriteProtectedFactSnapshot snapshot(RewriteProtectedFactExtractor self, NarrativeBlock block, String text, StyleMaskingLexicon lexicon)  {
        java.util.Objects.requireNonNull(block, "block");
        java.util.Objects.requireNonNull(text, "text");
        java.util.Objects.requireNonNull(lexicon, "lexicon");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        Map<FactKey, Integer> facts = new LinkedHashMap<>();
        TreeSet<String> manual = new TreeSet<>();

        RewriteProtectedFactExtractorAddLexicon.addLexicon(facts, ProtectedFactKind.NAME, normalized, lexicon.names());
        RewriteProtectedFactExtractorAddLexicon.addLexicon(facts, ProtectedFactKind.PLACE, normalized, lexicon.places());
        RewriteProtectedFactExtractorAddMatches.addMatches(facts, ProtectedFactKind.NUMBER, RewriteProtectedFactExtractor.NUMBER.matcher(normalized));
        RewriteProtectedFactExtractorAddMarkers.addMarkers(facts, ProtectedFactKind.NEGATION, normalized, RewriteProtectedFactExtractor.NEGATIONS);
        RewriteProtectedFactExtractorAddMarkers.addMarkers(facts, ProtectedFactKind.CAUSALITY, normalized, RewriteProtectedFactExtractor.CAUSALITY);
        RewriteProtectedFactExtractorAddMarkers.addMarkers(facts, ProtectedFactKind.PRESENCE, normalized, RewriteProtectedFactExtractor.PRESENCE);

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
}
