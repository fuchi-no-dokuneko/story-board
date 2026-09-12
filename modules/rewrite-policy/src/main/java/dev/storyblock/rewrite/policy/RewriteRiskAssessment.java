package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record RewriteRiskAssessment(
        Ids.ProposalId proposalId,
        String proposalHash,
        List<RewriteProtectedFactSnapshot> sourceFacts,
        List<RewriteProtectedFactSnapshot> candidateFacts,
        List<RewriteFactDifference> factDifferences,
        List<RewriteNearCopyFinding> nearCopyFindings,
        List<String> manualRiskReasons,
        RewriteRiskState state
) {
    static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public RewriteRiskAssessment {
        Objects.requireNonNull(proposalId, "proposalId");
        if (proposalHash == null || !HASH.matcher(proposalHash).matches()) {
            throw new IllegalArgumentException("Rewrite risk proposal hash is invalid");
        }
        sourceFacts = List.copyOf(sourceFacts);
        candidateFacts = List.copyOf(candidateFacts);
        factDifferences = List.copyOf(factDifferences);
        nearCopyFindings = List.copyOf(nearCopyFindings);
        manualRiskReasons = List.copyOf(manualRiskReasons);
        Objects.requireNonNull(state, "state");
        if (sourceFacts.size() != candidateFacts.size()
                || new HashSet<>(manualRiskReasons).size() != manualRiskReasons.size()
                || !manualRiskReasons.stream().sorted().toList().equals(
                        manualRiskReasons
                )) {
            throw new IllegalArgumentException("Rewrite risk assessment is not canonical");
        }
        boolean blocked = factDifferences.stream().anyMatch(value ->
                value.disposition() == RewriteFactDisposition.BLOCK)
                || nearCopyFindings.stream().anyMatch(value ->
                value.disposition() == NearCopyDisposition.BLOCK);
        boolean manual = !manualRiskReasons.isEmpty()
                || factDifferences.stream().anyMatch(value ->
                value.disposition() == RewriteFactDisposition.MANUAL_ONLY)
                || nearCopyFindings.stream().anyMatch(value ->
                value.disposition() == NearCopyDisposition.MANUAL_ONLY);
        RewriteRiskState expected = blocked ? RewriteRiskState.BLOCKED
                : manual ? RewriteRiskState.MANUAL_ONLY : RewriteRiskState.SAFE;
        if (state != expected) {
            throw new IllegalArgumentException(
                    "Rewrite risk state does not match its evidence"
            );
        }
    }

    public String assessmentHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        return RewriteRiskAssessmentCanonicalValueAction.canonicalValue(this);
    }
}
