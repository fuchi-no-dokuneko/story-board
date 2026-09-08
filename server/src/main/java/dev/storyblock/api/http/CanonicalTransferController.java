package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyStore;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.CanonicalImportResult;
import dev.storyblock.storage.ExportJobResult;
import dev.storyblock.storage.StoredArtifact;
import dev.storyblock.storage.StoredExportJob;
import dev.storyblock.storage.MissingExportJobException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/v1")
public final class CanonicalTransferController {
    public static final String ARTIFACT_CODEC_HEADER = "X-Artifact-Codec";

    private final CanonicalTransferService transfers;
    private final StyleAnalysisService analyses;
    private final AccessKeyStore securityStore;
    private final Clock clock;

    public CanonicalTransferController(
            CanonicalTransferService transfers,
            StyleAnalysisService analyses,
            AccessKeyStore securityStore,
            Clock clock
    ) {
        this.transfers = java.util.Objects.requireNonNull(transfers, "transfers");
        this.analyses = java.util.Objects.requireNonNull(analyses, "analyses");
        this.securityStore = java.util.Objects.requireNonNull(
                securityStore, "securityStore"
        );
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping("/imports")
    ResponseEntity<Map<String, Object>> importNovel(
            @RequestBody byte[] requestBytes,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        Map<String, Object> request = CanonicalTransferControllerParseObject.parseObject(requestBytes, "import request");
        CanonicalTransferControllerRequireKeys.requireKeys(request, Set.of("format", "document"), "import request");
        CanonicalExportFormat format = CanonicalExportFormat.fromCanonicalName(
                CanonicalTransferControllerString.string(request, "format", "import request")
        );
        Map<String, Object> documentObject = object(
                request.get("document"), "import request.document"
        );
        Ids.NovelId requestedNovel = CanonicalTransferControllerImportNovelId.importNovelId(format, documentObject);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        byte[] document = CanonicalJson.bytes(documentObject);
        Instant now = Instant.now(clock);
        AuditContext auditContext = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        CanonicalImportResult result = transfers.importDocument(
                format, document, idempotencyKey, now
        );
        securityStore.appendAuditEvent(AuditEvent.create(
                auditContext,
                result.novelId(),
                AuditAction.CANONICAL_IMPORT,
                result.head().revisionId().value(),
                null,
                result.head().revisionId(),
                result.idempotentReplay()
                        ? AuditResult.IDEMPOTENT : AuditResult.SUCCEEDED,
                null,
                result.head().contentHash()
        ));
        Map<String, Object> body = CanonicalTransferControllerNovelHead.novelHead(result);
        HttpStatus status = result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .location(URI.create("/v1/novels/" + result.novelId().value()))
                .header(HttpHeaders.ETAG, CanonicalTransferControllerQuotedEtag.quotedEtag(result.head().contentHash()))
                .body(body);
    }

    @PostMapping("/novels/{novelId}/exports")
    ResponseEntity<Map<String, Object>> startExport(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = CanonicalTransferControllerParseObject.parseObject(requestBytes, "export request");
        CanonicalTransferControllerRequireKeys.requireKeys(request, Set.of("revision_id", "format"), "export request");
        CanonicalExportFormat format = CanonicalExportFormat.fromCanonicalName(
                CanonicalTransferControllerString.string(request, "format", "export request")
        );
        Instant now = Instant.now(clock);
        AuditContext auditContext = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        ExportJobResult result = transfers.requestExport(
                requestedNovel,
                new Ids.RevisionId(CanonicalTransferControllerString.string(request, "revision_id", "export request")),
                CanonicalTransferControllerUnquoteEtag.unquoteEtag(ifMatch),
                format,
                idempotencyKey,
                now
        );
        securityStore.appendAuditEvent(AuditEvent.create(
                auditContext,
                requestedNovel,
                AuditAction.CANONICAL_EXPORT,
                result.job().jobId().value(),
                null,
                result.job().revision().revisionId(),
                result.idempotentReplay()
                        ? AuditResult.IDEMPOTENT : AuditResult.SUCCEEDED,
                null,
                result.job().revision().contentHash()
        ));
        String statusUri = "/v1/jobs/" + result.job().jobId().value();
        return ResponseEntity.accepted()
                .location(URI.create(statusUri))
                .body(Map.of(
                        "job_id", result.job().jobId().value(),
                        "status", "queued",
                        "status_uri", statusUri
                ));
    }

    @GetMapping("/jobs/{jobId}")
    ResponseEntity<Map<String, Object>> getJob(@PathVariable String jobId) {
        Ids.JobId id = new Ids.JobId(jobId);
        final StoredExportJob job;
        try {
            job = transfers.getExportJob(id);
        } catch (MissingExportJobException missingExport) {
            var analysis = analyses.getJob(id);
            return ResponseEntity.ok()
                    .eTag(analysis.statusHash())
                    .body(StyleAnalysisController.publicJob(analysis));
        }
        String artifactUri = "/v1/artifacts/" + job.resultArtifactId().value();
        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("job_id", job.jobId().value()),
                Map.entry("novel_id", job.novelId().value()),
                Map.entry("revision_id", job.revision().revisionId().value()),
                Map.entry("kind", StoredExportJob.KIND),
                Map.entry("status", StoredExportJob.STATUS),
                Map.entry("attempt", StoredExportJob.ATTEMPT),
                Map.entry("result_artifact_id", job.resultArtifactId().value()),
                Map.entry("result_uri", artifactUri),
                Map.entry("created_at", job.createdAt().toString()),
                Map.entry("updated_at", job.createdAt().toString())
        ));
    }

    @GetMapping("/artifacts/{artifactId}")
    ResponseEntity<byte[]> getArtifact(@PathVariable String artifactId) {
        Ids.ArtifactId id = new Ids.ArtifactId(artifactId);
        analyses.requireArtifactAvailable(id, Instant.now(clock));
        StoredArtifact artifact = transfers.getArtifact(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(artifact.mediaType()));
        headers.setContentLength(artifact.content().length);
        headers.set(HttpHeaders.ETAG, CanonicalTransferControllerQuotedEtag.quotedEtag(artifact.contentHash()));
        headers.setCacheControl("no-store");
        headers.set(ARTIFACT_CODEC_HEADER, artifact.codec());
        String extension = CanonicalTransferControllerArtifactExtension.artifactExtension(artifact);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(artifact.artifactId().value() + extension)
                .build());
        return new ResponseEntity<>(artifact.content(), headers, HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new CanonicalPackageException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new CanonicalPackageException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }

}
