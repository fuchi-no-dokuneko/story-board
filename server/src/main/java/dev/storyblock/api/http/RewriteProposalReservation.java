package dev.storyblock.api.http;

import dev.storyblock.application.RewriteGateService;
import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.RewritePolicyModule;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
final class RewriteProposalReservation {
    private final RewriteGateService rewrites;
    private final StyleAnalysisService analyses;
    private final Clock clock;

    RewriteProposalReservation(RewriteGateService rewrites, StyleAnalysisService analyses, Clock clock) {
        this.rewrites = rewrites;
        this.analyses = analyses;
        this.clock = clock;
    }

    ResponseEntity<Map<String, Object>> reserve(byte[] bytes, String ifMatch, String idempotencyKey,
            Authentication authentication, HttpServletRequest servletRequest) {
        var request = StrictJsonRequest.parseObject(bytes, "rewrite proposal request");
        RewriteProposalRequest.requireRequestFields(request);
        var analysisId = new Ids.StyleAnalysisId(StrictJsonRequest.string(
                request, "analysis_id", "rewrite proposal request"));
        var novelId = new Ids.NovelId(StrictJsonRequest.string(
                request, "novel_id", "rewrite proposal request"));
        AccessPrincipalSupport.requireNovel(authentication, novelId);
        var analysis = analyses.getAnalysis(analysisId);
        RewriteProposalRequest.requireRequestBinding(request, analysis, novelId);
        Duration cooldown = request.get("cooldown_seconds") == null
                ? RewritePolicyModule.DEFAULT_COOLDOWN
                : Duration.ofSeconds(StrictJsonRequest.integer(
                        request, "cooldown_seconds", "rewrite proposal request"));
        var audit = AccessPrincipalSupport.auditContext(authentication, servletRequest, Instant.now(clock));
        var result = rewrites.reserve(analysisId, StrictJsonRequest.unquoteEtag(ifMatch),
                RewriteProposalRequest.strings(request.get("finding_ids")), cooldown, idempotencyKey, audit);
        var reservation = result.reservation();
        String uri = "/v1/rewrite-proposals/" + reservation.proposalId().value();
        return ResponseEntity.accepted().location(URI.create(uri)).eTag(reservation.reservationHash())
                .body(Map.of("idempotent_replay", result.idempotentReplay(),
                        "proposal_id", reservation.proposalId().value(), "status", "pending",
                        "status_uri", uri, "worker_input_hash", reservation.workerInput().inputHash()));
    }
}
