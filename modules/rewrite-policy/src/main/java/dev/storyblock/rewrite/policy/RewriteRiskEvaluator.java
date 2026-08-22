package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteSourceBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.style.StyleProfileVersionView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class RewriteRiskEvaluator {
    private static final Set<ProtectedFactKind> SOURCE_MANUAL_RISK = Set.of(
            ProtectedFactKind.NUMBER,
            ProtectedFactKind.NEGATION,
            ProtectedFactKind.CAUSALITY,
            ProtectedFactKind.SPEAKER,
            ProtectedFactKind.PRESENCE,
            ProtectedFactKind.EVIDENCE,
            ProtectedFactKind.HIGH_RISK_METADATA
    );

    private final RewriteProtectedFactExtractor facts;
    private final RewriteNearCopyChecker nearCopy;

    public RewriteRiskEvaluator() {
        this(new RewriteProtectedFactExtractor(), new RewriteNearCopyChecker());
    }

    RewriteRiskEvaluator(
            RewriteProtectedFactExtractor facts,
            RewriteNearCopyChecker nearCopy
    ) {
        this.facts = java.util.Objects.requireNonNull(facts, "facts");
        this.nearCopy = java.util.Objects.requireNonNull(nearCopy, "nearCopy");
    }

    public RewriteRiskAssessment evaluate(
            RewriteTextProposal proposal,
            List<NarrativeBlock> sourceBlocks,
            StyleMaskingLexicon lexicon,
            StyleProfileVersionView currentProfile,
            List<RewriteReferenceCorpus> corpora
    ) {
        java.util.Objects.requireNonNull(proposal, "proposal");
        sourceBlocks = List.copyOf(sourceBlocks);
        requireExactSources(proposal, sourceBlocks);
        Map<Ids.BlockId, NarrativeBlock> sourceById = new HashMap<>();
        sourceBlocks.forEach(block -> sourceById.put(block.id(), block));

        List<RewriteProtectedFactSnapshot> sourceFacts = new ArrayList<>();
        List<RewriteProtectedFactSnapshot> candidateFacts = new ArrayList<>();
        List<RewriteFactDifference> differences = new ArrayList<>();
        TreeSet<String> manualReasons = new TreeSet<>();
        for (RewriteCandidateBlock candidate : proposal.candidates()) {
            NarrativeBlock source = sourceById.get(candidate.blockId());
            RewriteProtectedFactSnapshot before = facts.snapshot(
                    source, source.text(), lexicon
            );
            RewriteProtectedFactSnapshot after = facts.snapshot(
                    source, candidate.proposedText(), lexicon
            );
            sourceFacts.add(before);
            candidateFacts.add(after);
            differences.addAll(compare(before, after));
            manualReasons.addAll(before.manualRiskReasons());
            before.facts().stream()
                    .map(RewriteProtectedFact::kind)
                    .filter(SOURCE_MANUAL_RISK::contains)
                    .map(kind -> "high_risk:" + kind.canonicalName())
                    .forEach(manualReasons::add);
        }
        differences.sort(Comparator
                .comparing((RewriteFactDifference value) -> value.blockId().value())
                .thenComparing(value -> value.kind().ordinal())
                .thenComparing(RewriteFactDifference::valueHash));
        List<RewriteNearCopyFinding> nearCopyFindings = nearCopy.check(
                proposal, currentProfile, corpora
        );
        boolean blocked = differences.stream().anyMatch(value ->
                value.disposition() == RewriteFactDisposition.BLOCK)
                || nearCopyFindings.stream().anyMatch(value ->
                value.disposition() == NearCopyDisposition.BLOCK);
        boolean manual = !manualReasons.isEmpty()
                || differences.stream().anyMatch(value ->
                value.disposition() == RewriteFactDisposition.MANUAL_ONLY)
                || nearCopyFindings.stream().anyMatch(value ->
                value.disposition() == NearCopyDisposition.MANUAL_ONLY);
        return new RewriteRiskAssessment(
                proposal.proposalId(),
                proposal.proposalHash(),
                sourceFacts,
                candidateFacts,
                differences,
                nearCopyFindings,
                List.copyOf(manualReasons),
                blocked ? RewriteRiskState.BLOCKED
                        : manual ? RewriteRiskState.MANUAL_ONLY
                        : RewriteRiskState.SAFE
        );
    }

    private static void requireExactSources(
            RewriteTextProposal proposal,
            List<NarrativeBlock> sourceBlocks
    ) {
        List<RewriteSourceBlock> expected = proposal.input().blocks();
        if (sourceBlocks.size() != expected.size()) {
            throw new RewriteRiskPolicyException(
                    "Rewrite source metadata does not cover the exact worker input"
            );
        }
        for (int index = 0; index < expected.size(); index++) {
            RewriteSourceBlock binding = expected.get(index);
            NarrativeBlock source = sourceBlocks.get(index);
            if (!source.id().equals(binding.blockId())
                    || !source.versionId().equals(binding.blockVersionId())
                    || !source.text().equals(binding.text())
                    || !CanonicalJson.hash(source.text()).equals(binding.textHash())) {
                throw new RewriteRiskPolicyException(
                        "Rewrite source metadata does not match immutable worker input"
                );
            }
        }
    }

    private static List<RewriteFactDifference> compare(
            RewriteProtectedFactSnapshot before,
            RewriteProtectedFactSnapshot after
    ) {
        Map<FactKey, Integer> source = index(before.facts());
        Map<FactKey, Integer> candidate = index(after.facts());
        LinkedHashSet<FactKey> keys = new LinkedHashSet<>(source.keySet());
        keys.addAll(candidate.keySet());
        return keys.stream()
                .filter(key -> !source.getOrDefault(key, 0).equals(
                        candidate.getOrDefault(key, 0)
                ))
                .map(key -> new RewriteFactDifference(
                        before.blockId(),
                        key.kind(),
                        key.valueHash(),
                        source.getOrDefault(key, 0),
                        candidate.getOrDefault(key, 0),
                        key.kind().changedDisposition()
                ))
                .toList();
    }

    private static Map<FactKey, Integer> index(List<RewriteProtectedFact> values) {
        Map<FactKey, Integer> result = new HashMap<>();
        values.forEach(value -> result.put(
                new FactKey(value.kind(), value.valueHash()), value.count()
        ));
        return result;
    }

    private record FactKey(ProtectedFactKind kind, String valueHash) {
    }
}
