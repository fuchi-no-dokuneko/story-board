package dev.storyblock.api.http;

import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisLease;
import dev.storyblock.style.StyleAnalysisResult;
import dev.storyblock.style.StyleAnalysisSummary;
import dev.storyblock.style.StyleAnalysisTrace;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleMaskingLexicon;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public final class StyleAnalysisController {
    private static final Set<String> CREATE_FIELDS = Set.of(
            "revision_id", "profile_id", "profile_version_id", "from_block_id",
            "to_block_id", "masking_lexicon", "max_attempts", "retention_days"
    );
    private static final Set<String> CLAIM_FIELDS = Set.of(
            "novel_id", "lease_owner", "lease_seconds"
    );
    private static final Set<String> RESULT_FIELDS = Set.of(
            "lease_owner", "attempt", "snapshot_hash", "profile_version_hash",
            "analyzer_contract_hash", "window_configuration_hash", "summary",
            "windows", "trace", "completed_at"
    );
    private static final Set<String> COMPRESSED_TRACE_FIELDS = Set.of(
            "codec", "content_base64", "content_hash", "uncompressed_bytes"
    );

    private final StyleAnalysisService analyses;
    private final Clock clock;

    public StyleAnalysisController(StyleAnalysisService analyses, Clock clock) {
        this.analyses = java.util.Objects.requireNonNull(analyses, "analyses");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping("/novels/{novelId}/style-analyses")
    ResponseEntity<Map<String, Object>> create(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "style analysis request"
        );
        requireFields(
                request,
                Set.of("revision_id", "profile_id", "profile_version_id"),
                CREATE_FIELDS,
                "style analysis request"
        );
        StyleMaskingLexicon lexicon = request.containsKey("masking_lexicon")
                ? StyleMaskingLexicon.fromCanonical(StrictJsonRequest.object(
                        request.get("masking_lexicon"),
                        "style analysis request.masking_lexicon"
                ))
                : StyleMaskingLexicon.empty();
        int maxAttempts = request.containsKey("max_attempts")
                ? StrictJsonRequest.integer(
                        request, "max_attempts", "style analysis request"
                ) : StyleAnalysisService.DEFAULT_MAX_ATTEMPTS;
        int retentionDays = request.containsKey("retention_days")
                ? StrictJsonRequest.integer(
                        request, "retention_days", "style analysis request"
                ) : Math.toIntExact(StyleAnalysisService.DEFAULT_RETENTION.toDays());
        Instant now = Instant.now(clock);
        var result = analyses.request(
                requestedNovel,
                new Ids.RevisionId(StrictJsonRequest.string(
                        request, "revision_id", "style analysis request"
                )),
                StrictJsonRequest.unquoteEtag(ifMatch),
                new Ids.StyleProfileId(StrictJsonRequest.string(
                        request, "profile_id", "style analysis request"
                )),
                new Ids.StyleProfileVersionId(StrictJsonRequest.string(
                        request, "profile_version_id", "style analysis request"
                )),
                optionalBlockId(request.get("from_block_id"), "from_block_id"),
                optionalBlockId(request.get("to_block_id"), "to_block_id"),
                lexicon,
                maxAttempts,
                Duration.ofDays(retentionDays),
                idempotencyKey,
                AccessPrincipalSupport.auditContext(
                        authentication, servletRequest, now
                )
        );
        StyleAnalysisJob job = result.job();
        String statusUri = "/v1/jobs/" + job.jobId().value();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("analysis_id", job.analysisId().value());
        body.put("analysis_uri", "/v1/style-analyses/" + job.analysisId().value());
        body.put("idempotent_replay", result.idempotentReplay());
        body.put("job_id", job.jobId().value());
        body.put("status", job.status().canonicalName());
        body.put("status_uri", statusUri);
        return ResponseEntity.accepted()
                .location(URI.create(statusUri))
                .eTag(job.statusHash())
                .body(body);
    }

    @GetMapping("/style-analyses/{analysisId}")
    ResponseEntity<Map<String, Object>> getAnalysis(@PathVariable String analysisId) {
        Ids.StyleAnalysisId id = new Ids.StyleAnalysisId(analysisId);
        StyleAnalysisJob job = analyses.getAnalysis(id);
        Map<String, Object> body = publicJob(job);
        Optional<StyleAnalysisResult> result = analyses.result(id);
        result.ifPresent(value -> {
            body.put("result", value.canonicalValue());
            body.put("trace_uri", "/v1/artifacts/" + value.traceArtifactId().value());
            body.put("windows_uri", "/v1/style-analyses/" + analysisId + "/windows");
        });
        return ResponseEntity.ok().eTag(job.statusHash()).body(body);
    }

    @GetMapping("/style-analyses/{analysisId}/windows")
    ResponseEntity<Map<String, Object>> windows(
            @PathVariable String analysisId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(analyses.windows(
                new Ids.StyleAnalysisId(analysisId), cursor, limit
        ).canonicalValue());
    }

    @PostMapping("/internal/jobs/claims")
    ResponseEntity<Map<String, Object>> claim(
            @RequestBody byte[] requestBytes,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication
    ) {
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "style job claim"
        );
        StrictJsonRequest.requireKeys(request, CLAIM_FIELDS, "style job claim");
        Ids.NovelId novelId = new Ids.NovelId(StrictJsonRequest.string(
                request, "novel_id", "style job claim"
        ));
        AccessPrincipalSupport.requireNovel(authentication, novelId);
        Optional<StyleAnalysisLease> lease = analyses.claim(
                novelId,
                StrictJsonRequest.string(request, "lease_owner", "style job claim"),
                Duration.ofSeconds(StrictJsonRequest.integer(
                        request, "lease_seconds", "style job claim"
                )),
                idempotencyKey,
                Instant.now(clock)
        );
        if (lease.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .eTag(lease.get().claimedStatusHash())
                .body(lease.get().canonicalValue());
    }

    @PostMapping("/internal/jobs/{jobId}/results")
    ResponseEntity<Map<String, Object>> complete(
            @PathVariable String jobId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication
    ) {
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "style job result"
        );
        StrictJsonRequest.requireKeys(request, RESULT_FIELDS, "style job result");
        StyleAnalysisJob job = analyses.getJob(new Ids.JobId(jobId));
        AccessPrincipalSupport.requireNovel(
                authentication, job.snapshot().novelId()
        );
        Instant completedAt = StrictJsonRequest.instant(
                request, "completed_at", "style job result"
        );
        if (completedAt.isAfter(Instant.now(clock).plusSeconds(30))) {
            throw new IllegalArgumentException(
                    "style job result.completed_at cannot be in the future"
            );
        }
        StyleAnalysisTrace trace = parseTrace(
                job,
                StrictJsonRequest.object(
                        request.get("trace"), "style job result.trace"
                ),
                completedAt
        );
        var result = analyses.complete(new StyleAnalysisCompletionCommand(
                job.jobId(),
                StrictJsonRequest.string(
                        request, "lease_owner", "style job result"
                ),
                StrictJsonRequest.integer(request, "attempt", "style job result"),
                StrictJsonRequest.unquoteEtag(ifMatch),
                StrictJsonRequest.string(
                        request, "snapshot_hash", "style job result"
                ),
                StrictJsonRequest.string(
                        request, "profile_version_hash", "style job result"
                ),
                StrictJsonRequest.string(
                        request, "analyzer_contract_hash", "style job result"
                ),
                StrictJsonRequest.string(
                        request, "window_configuration_hash", "style job result"
                ),
                StyleAnalysisSummary.fromCanonical(StrictJsonRequest.object(
                        request.get("summary"), "style job result.summary"
                )),
                StrictJsonRequest.objects(
                        request.get("windows"), "style job result.windows"
                ).stream().map(StyleAnalysisWindowFinding::fromCanonical).toList(),
                trace,
                idempotencyKey,
                completedAt
        ));
        return ResponseEntity.status(HttpStatus.OK)
                .eTag(result.job().statusHash())
                .body(Map.of(
                        "analysis_id", result.result().analysisId().value(),
                        "idempotent_replay", result.idempotentReplay(),
                        "job_id", result.job().jobId().value(),
                        "result_hash", result.result().resultHash(),
                        "status", result.job().status().canonicalName()
                ));
    }

    static Map<String, Object> publicJob(StyleAnalysisJob job) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", job.analysisId().value());
        value.put("attempt", job.attempt());
        value.put("created_at", job.createdAt().toString());
        value.put("failure_code", job.failureCode());
        value.put("job_id", job.jobId().value());
        value.put("kind", "style-analysis");
        value.put("max_attempts", job.maxAttempts());
        value.put("novel_id", job.snapshot().novelId().value());
        value.put("result_artifact_id", job.resultArtifactId() == null
                ? null : job.resultArtifactId().value());
        value.put("result_hash", job.resultHash());
        value.put("retention_until", job.retentionUntil().toString());
        value.put("revision_id", job.snapshot().revisionId().value());
        value.put("status", job.status().canonicalName());
        value.put("updated_at", job.updatedAt().toString());
        return value;
    }

    private static Ids.BlockId optionalBlockId(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(
                    "style analysis request." + field + " must be a string or null"
            );
        }
        return new Ids.BlockId(text);
    }

    private static StyleAnalysisTrace parseTrace(
            StyleAnalysisJob job,
            Map<String, Object> trace,
            Instant completedAt
    ) {
        if (!trace.keySet().equals(COMPRESSED_TRACE_FIELDS)) {
            return StyleAnalysisTrace.create(
                    job.analysisId(), trace, completedAt, job.retentionUntil()
            );
        }
        String codec = StrictJsonRequest.string(
                trace, "codec", "style job result.trace"
        );
        if (!StyleAnalysisTrace.CODEC.equals(codec)) {
            throw new IllegalArgumentException(
                    "style job result.trace.codec must be gzip"
            );
        }
        final byte[] compressed;
        try {
            compressed = Base64.getDecoder().decode(StrictJsonRequest.string(
                    trace, "content_base64", "style job result.trace"
            ));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "style job result.trace.content_base64 is invalid", failure
            );
        }
        return StyleAnalysisTrace.fromCompressed(
                job.analysisId(),
                StrictJsonRequest.string(
                        trace, "content_hash", "style job result.trace"
                ),
                compressed,
                StrictJsonRequest.integer(
                        trace, "uncompressed_bytes", "style job result.trace"
                ),
                completedAt,
                job.retentionUntil()
        );
    }

    private static void requireFields(
            Map<String, Object> request,
            Set<String> required,
            Set<String> allowed,
            String path
    ) {
        for (String field : required) {
            if (!request.containsKey(field)) {
                throw new IllegalArgumentException(path + " is missing " + field);
            }
        }
        for (String field : request.keySet()) {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException(path + " contains unknown field " + field);
            }
        }
    }
}
