package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyStore;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.CanonicalImportResult;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StoredRevision;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/novels")
public final class NovelController {
    private final CanonicalTransferService transfers;
    private final AccessKeyStore auditStore;
    private final Clock clock;

    public NovelController(
            CanonicalTransferService transfers,
            AccessKeyStore auditStore,
            Clock clock
    ) {
        this.transfers = java.util.Objects.requireNonNull(transfers, "transfers");
        this.auditStore = java.util.Objects.requireNonNull(auditStore, "auditStore");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> create(
            @RequestBody byte[] requestBytes,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY)
            String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "create novel request"
        );
        StrictJsonRequest.requireKeys(
                request, Set.of("initial_revision"), "create novel request"
        );
        Map<String, Object> initial = StrictJsonRequest.object(
                request.get("initial_revision"), "create novel request.initial_revision"
        );
        CanonicalRevision revision = CanonicalRevision.parseEnvelope(
                CanonicalJson.bytes(initial)
        );
        Ids.NovelId novelId = new Ids.NovelId((String) revision
                .canonicalContent().get("novel_id"));
        AccessPrincipalSupport.requireNovel(authentication, novelId);
        Instant now = Instant.now(clock);
        CanonicalImportResult result = transfers.importDocument(
                CanonicalExportFormat.REVISION,
                revision.envelopeBytes(),
                idempotencyKey,
                now
        );
        AuditContext audit = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        auditStore.appendAuditEvent(AuditEvent.create(
                audit,
                novelId,
                AuditAction.CANONICAL_IMPORT,
                result.head().revisionId().value(),
                null,
                result.head().revisionId(),
                result.idempotentReplay()
                        ? AuditResult.IDEMPOTENT : AuditResult.SUCCEEDED,
                null,
                result.head().contentHash()
        ));
        HttpStatus status = result.idempotentReplay()
                ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .location(URI.create("/v1/novels/" + novelId.value()))
                .eTag(result.head().contentHash())
                .body(head(novelId, result.head()));
    }

    @GetMapping("/{novelId}")
    ResponseEntity<Map<String, Object>> head(
            @PathVariable String novelId,
            Authentication authentication
    ) {
        Ids.NovelId id = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, id);
        RevisionRef head = transfers.getHead(id);
        return ResponseEntity.ok().eTag(head.contentHash()).body(head(id, head));
    }

    @GetMapping("/{novelId}/revisions/{revisionId}")
    ResponseEntity<Map<String, Object>> revision(
            @PathVariable String novelId,
            @PathVariable String revisionId,
            Authentication authentication
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        StoredRevision stored = transfers.getRevision(
                requestedNovel, new Ids.RevisionId(revisionId)
        );
        CanonicalRevision canonical = NarrativeCanonicalMapper.toCanonical(
                stored.manifest()
        );
        return ResponseEntity.ok().eTag(canonical.contentHash()).body(
                canonical.envelope()
        );
    }

    private static Map<String, Object> head(Ids.NovelId novelId, RevisionRef head) {
        return Map.of(
                "head_hash", head.contentHash(),
                "head_revision_id", head.revisionId().value(),
                "head_sequence", head.sequence(),
                "novel_id", novelId.value(),
                "schema_version", CanonicalRevision.SCHEMA_VERSION
        );
    }
}
