package dev.storyblock.api.http;

import dev.storyblock.application.RewriteGateService;
import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteCandidateReservationSaveResult;
import dev.storyblock.rewrite.policy.RewritePolicyModule;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleAnalysisJob;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rewrite-proposals")
public final class RewriteProposalController {
    private static final Set<String> REQUEST_FIELDS = Set.of(
            "analysis_id", "novel_id", "revision_id", "revision_hash",
            "finding_ids", "cooldown_seconds"
    );

    private final RewriteGateService rewrites;
    private final StyleAnalysisService analyses;
    private final Clock clock;

    public RewriteProposalController(
            RewriteGateService rewrites,
            StyleAnalysisService analyses,
            Clock clock
    ) {
        this.rewrites = java.util.Objects.requireNonNull(rewrites, "rewrites");
        this.analyses = java.util.Objects.requireNonNull(analyses, "analyses");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> reserve(
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY)
            String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "rewrite proposal request"
        );
        requireRequestFields(request);
        Ids.StyleAnalysisId analysisId = new Ids.StyleAnalysisId(
                StrictJsonRequest.string(
                        request, "analysis_id", "rewrite proposal request"
                )
        );
        Ids.NovelId novelId = new Ids.NovelId(StrictJsonRequest.string(
                request, "novel_id", "rewrite proposal request"
        ));
        AccessPrincipalSupport.requireNovel(authentication, novelId);
        StyleAnalysisJob analysis = analyses.getAnalysis(analysisId);
        requireRequestBinding(request, analysis, novelId);
        Duration cooldown = request.get("cooldown_seconds") == null
                ? RewritePolicyModule.DEFAULT_COOLDOWN
                : Duration.ofSeconds(StrictJsonRequest.integer(
                        request, "cooldown_seconds", "rewrite proposal request"
                ));
        Instant now = Instant.now(clock);
        AuditContext audit = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        RewriteCandidateReservationSaveResult result = rewrites.reserve(
                analysisId,
                StrictJsonRequest.unquoteEtag(ifMatch),
                strings(request.get("finding_ids")),
                cooldown,
                idempotencyKey,
                audit
        );
        RewriteCandidateReservation reservation = result.reservation();
        String uri = "/v1/rewrite-proposals/" + reservation.proposalId().value();
        return ResponseEntity.accepted()
                .location(URI.create(uri))
                .eTag(reservation.reservationHash())
                .body(Map.of(
                        "idempotent_replay", result.idempotentReplay(),
                        "proposal_id", reservation.proposalId().value(),
                        "status", "pending",
                        "status_uri", uri,
                        "worker_input_hash", reservation.workerInput().inputHash()
                ));
    }

    @GetMapping("/{proposalId}")
    ResponseEntity<Map<String, Object>> get(@PathVariable String proposalId) {
        RewriteCandidateReservation reservation = rewrites.get(
                new Ids.ProposalId(proposalId)
        );
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", reservation.eligibility().analysisId().value());
        value.put("cooldown_until", reservation.cooldownUntil().toString());
        value.put("finding_ids", reservation.eligibility().findingIds());
        value.put("novel_id", reservation.novelId().value());
        value.put("proposal_id", reservation.proposalId().value());
        value.put("reservation_hash", reservation.reservationHash());
        value.put("revision_hash", reservation.eligibility().revisionHash());
        value.put("revision_id", reservation.eligibility().revisionId().value());
        value.put("status", "pending");
        value.put("worker_input", reservation.workerInput().canonicalValue());
        return ResponseEntity.ok().eTag(reservation.reservationHash()).body(value);
    }

    private static void requireRequestBinding(
            Map<String, Object> request,
            StyleAnalysisJob analysis,
            Ids.NovelId novelId
    ) {
        if (!analysis.snapshot().novelId().equals(novelId)
                || !analysis.snapshot().revisionId().value().equals(
                        StrictJsonRequest.string(
                                request, "revision_id", "rewrite proposal request"
                        )
                )
                || !analysis.snapshot().revisionHash().equals(
                        StrictJsonRequest.string(
                                request, "revision_hash", "rewrite proposal request"
                        )
                )) {
            throw new IllegalArgumentException(
                    "Rewrite proposal request does not match its style analysis"
            );
        }
    }

    private static void requireRequestFields(Map<String, Object> request) {
        for (String required : Set.of(
                "analysis_id", "novel_id", "revision_id", "revision_hash",
                "finding_ids"
        )) {
            if (!request.containsKey(required)) {
                throw new IllegalArgumentException(
                        "rewrite proposal request is missing " + required
                );
            }
        }
        for (String field : request.keySet()) {
            if (!REQUEST_FIELDS.contains(field)) {
                throw new IllegalArgumentException(
                        "rewrite proposal request contains unknown field " + field
                );
            }
        }
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> entries)) {
            throw new IllegalArgumentException(
                    "rewrite proposal request.finding_ids must be an array"
            );
        }
        return entries.stream().map(entry -> {
            if (!(entry instanceof String text)) {
                throw new IllegalArgumentException(
                        "rewrite proposal request.finding_ids must contain strings"
                );
            }
            return text;
        }).toList();
    }
}
