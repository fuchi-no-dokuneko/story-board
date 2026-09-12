package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.ExportJobResult;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class CanonicalTransferControllerStartExportAction {
    static ResponseEntity<Map<String, Object>> startExport(CanonicalTransferController self, String novelId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = CanonicalTransferControllerParseObject.parseObject(requestBytes, "export request");
        CanonicalTransferControllerRequireKeys.requireKeys(request, Set.of("revision_id", "format"), "export request");
        CanonicalExportFormat format = CanonicalExportFormat.fromCanonicalName(
                CanonicalTransferControllerString.string(request, "format", "export request")
        );
        Instant now = Instant.now(self.clock);
        AuditContext auditContext = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        ExportJobResult result = self.transfers.requestExport(
                requestedNovel,
                new Ids.RevisionId(CanonicalTransferControllerString.string(request, "revision_id", "export request")),
                CanonicalTransferControllerUnquoteEtag.unquoteEtag(ifMatch),
                format,
                idempotencyKey,
                now
        );
        self.securityStore.appendAuditEvent(AuditEvent.create(
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
}
