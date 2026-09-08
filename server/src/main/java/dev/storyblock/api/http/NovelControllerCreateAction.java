package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.CanonicalImportResult;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class NovelControllerCreateAction {
    static ResponseEntity<Map<String, Object>> create(NovelController self, byte[] requestBytes, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
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
        Instant now = Instant.now(self.clock);
        CanonicalImportResult result = self.transfers.importDocument(
                CanonicalExportFormat.REVISION,
                revision.envelopeBytes(),
                idempotencyKey,
                now
        );
        AuditContext audit = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        self.auditStore.appendAuditEvent(AuditEvent.create(
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
                .body(self.head(novelId, result.head()));
    }
}
