package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.*;
import dev.storyblock.storage.CanonicalImportResult;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;

final class CanonicalTransferControllerImportNovelAction {
  static ResponseEntity<Map<String, Object>> importNovel(CanonicalTransferController self, byte[] requestBytes, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
    Map<String, Object> request = CanonicalTransferControllerParseObject.parseObject(requestBytes, "import request");
    CanonicalTransferControllerRequireKeys.requireKeys(request, Set.of("format", "document"), "import request");
    CanonicalExportFormat format = CanonicalExportFormat.fromCanonicalName(
        CanonicalTransferControllerString.string(request, "format", "import request")
    );
    Map<String, Object> documentObject = CanonicalTransferController.object(
        request.get("document"), "import request.document"
    );
    Ids.NovelId requestedNovel = CanonicalTransferControllerImportNovelId.importNovelId(format, documentObject);
    AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
    byte[] document = CanonicalJson.bytes(documentObject);
    Instant now = Instant.now(self.clock);
    AuditContext auditContext = AccessPrincipalSupport.auditContext(
        authentication, servletRequest, now
    );
    CanonicalImportResult result = self.transfers.importDocument(
        format, document, idempotencyKey, now
    );
    self.securityStore.appendAuditEvent(AuditEvent.create(
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
}
