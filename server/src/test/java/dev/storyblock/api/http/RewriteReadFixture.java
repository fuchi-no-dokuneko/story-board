package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.*;
import java.time.Instant;
import java.util.List;

final class RewriteReadFixture {
    static RewriteCandidateReservation reservation() {
        var source = RewriteSourceBlock.create(Ids.BlockId.create(), Ids.BlockVersionId.create(), "春風吹過河岸。", true);
        String finding = CanonicalJson.hash("operational-window");
        var decision = new StyleAnomalyDecision(
                StyleDecisionState.REWRITE_CANDIDATE,
                StyleDecisionReason.SUSTAINED_MULTI_CHANNEL_Q99,
                StyleCalibrationConfidence.CALIBRATED, finding,
                List.of(StyleFeatureChannel.GRAMMAR, StyleFeatureChannel.RHYTHM),
                List.of(CanonicalJson.hash("sustaining-first"), CanonicalJson.hash("sustaining-second")),
                List.of(), false, true);
        var eligibility = new RewriteEligibility(
                Ids.StyleAnalysisId.create(), CanonicalJson.hash("analysis-result"),
                Ids.NovelId.create(), Ids.RevisionId.create(), CanonicalJson.hash(source.canonicalValue()),
                Ids.StyleProfileId.create(), Ids.StyleProfileVersionId.create(), CanonicalJson.hash("ready-profile"),
                CanonicalJson.hash("analyzer-contract"), CanonicalJson.hash("window-configuration"),
                List.of(finding), List.of(source.blockId()), List.of(decision));
        var input = new RewriteWorkerInput(
                Ids.ProposalId.create(), eligibility.analysisId(), eligibility.novelId(),
                eligibility.revisionId(), eligibility.revisionHash(), eligibility.profileVersionId(),
                eligibility.profileVersionHash(), eligibility.analyzerContractHash(),
                eligibility.windowConfigurationHash(), eligibility.findingIds(), List.of(source),
                new RewriteConstraints(1, 100, List.of("Preserve meaning and improve rhythm")));
        Instant now = Instant.parse("2026-09-08T17:00:00Z");
        return new RewriteCandidateReservation(eligibility, input,
                AuditContext.system("req_rewrite_read", now), now.plus(RewritePolicyModule.DEFAULT_COOLDOWN));
    }
}
