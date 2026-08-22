package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisBlock;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisJobStatus;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleAnomalyDecision;
import dev.storyblock.style.StyleProfileVersionView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RewriteEligibilityPolicy {
    public RewriteEligibility evaluate(
            StyleAnalysisJob analysis,
            StyleProfileVersionView currentProfile,
            List<StyleAnalysisWindowFinding> selectedFindings,
            Instant evaluatedAt
    ) {
        Objects.requireNonNull(analysis, "analysis");
        Objects.requireNonNull(currentProfile, "currentProfile");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt");
        if (analysis.status() != StyleAnalysisJobStatus.SUCCEEDED
                || analysis.resultHash() == null
                || !evaluatedAt.isBefore(analysis.retentionUntil())) {
            throw new RewriteEligibilityException(
                    "Rewrite requires a completed retained style analysis"
            );
        }
        var snapshotVersion = analysis.snapshot().profileVersion();
        var currentVersion = currentProfile.profileVersion();
        if (!currentProfile.canGateRewrites()
                || !snapshotVersion.profileId().equals(currentVersion.profileId())
                || !snapshotVersion.versionId().equals(currentVersion.versionId())
                || !snapshotVersion.versionHash().equals(currentVersion.versionHash())) {
            throw new RewriteEligibilityException(
                    "Rewrite requires the analysis profile version to be approved and READY"
            );
        }

        selectedFindings = new ArrayList<>(List.copyOf(selectedFindings));
        selectedFindings.sort(Comparator.comparingInt(
                StyleAnalysisWindowFinding::ordinal
        ));
        if (selectedFindings.isEmpty()
                || new HashSet<>(selectedFindings.stream().map(
                        StyleAnalysisWindowFinding::windowId
                ).toList()).size() != selectedFindings.size()
                || new HashSet<>(selectedFindings.stream().map(
                        StyleAnalysisWindowFinding::ordinal
                ).toList()).size() != selectedFindings.size()) {
            throw new RewriteEligibilityException(
                    "Rewrite findings must be nonempty and unique"
            );
        }

        Map<Ids.BlockId, Integer> snapshotOrdinals = new HashMap<>();
        List<StyleAnalysisBlock> snapshotBlocks = analysis.snapshot().blocks();
        for (int index = 0; index < snapshotBlocks.size(); index++) {
            snapshotOrdinals.put(snapshotBlocks.get(index).block().id(), index);
        }
        LinkedHashSet<Ids.BlockId> selectedBlocks = new LinkedHashSet<>();
        List<StyleAnomalyDecision> decisions = new ArrayList<>();
        for (StyleAnalysisWindowFinding finding : selectedFindings) {
            StyleAnomalyDecision decision = decision(finding);
            if (!finding.windowId().equals(decision.operationalWindowId())
                    || !finding.canTriggerRewrite()
                    || finding.decisionState() != decision.state()
                    || finding.confidence() != decision.confidence()) {
                throw new RewriteEligibilityException(
                        "Rewrite finding does not match its immutable anomaly decision"
                );
            }
            for (Ids.BlockId blockId : finding.blockIds()) {
                if (!snapshotOrdinals.containsKey(blockId)) {
                    throw new RewriteEligibilityException(
                            "Rewrite finding references a block outside the analysis snapshot"
                    );
                }
                selectedBlocks.add(blockId);
            }
            decisions.add(decision);
        }
        int first = selectedBlocks.stream().mapToInt(snapshotOrdinals::get).min()
                .orElseThrow();
        int last = selectedBlocks.stream().mapToInt(snapshotOrdinals::get).max()
                .orElseThrow();
        List<Ids.BlockId> affected = snapshotBlocks.subList(first, last + 1).stream()
                .map(block -> block.block().id()).toList();
        if (affected.size() > dev.storyblock.rewrite.RewriteModule.MAX_EDITABLE_BLOCKS) {
            throw new RewriteEligibilityException(
                    "Rewrite findings do not resolve to a minimal bounded range"
            );
        }
        return new RewriteEligibility(
                analysis.analysisId(),
                analysis.resultHash(),
                analysis.snapshot().novelId(),
                analysis.snapshot().revisionId(),
                analysis.snapshot().revisionHash(),
                currentVersion.profileId(),
                currentVersion.versionId(),
                currentVersion.versionHash(),
                analysis.snapshot().analyzerContractHash(),
                analysis.snapshot().windowConfigurationHash(),
                selectedFindings.stream().map(
                        StyleAnalysisWindowFinding::windowId
                ).toList(),
                affected,
                decisions
        );
    }

    private static StyleAnomalyDecision decision(
            StyleAnalysisWindowFinding finding
    ) {
        Map<String, Object> payload = finding.payload();
        if (!payload.keySet().equals(Set.of("decision"))
                || !(payload.get("decision") instanceof Map<?, ?> raw)) {
            throw new RewriteEligibilityException(
                    "Rewrite finding lacks an exact anomaly decision payload"
            );
        }
        Map<String, Object> value = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new RewriteEligibilityException(
                        "Rewrite anomaly decision contains a non-string field"
                );
            }
            value.put(key, entry.getValue());
        }
        try {
            return StyleAnomalyDecision.fromCanonical(value);
        } catch (IllegalArgumentException failure) {
            throw new RewriteEligibilityException(
                    "Rewrite anomaly decision is not eligible"
            );
        }
    }
}
