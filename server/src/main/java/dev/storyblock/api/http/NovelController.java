package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyStore;
import dev.storyblock.storage.RevisionRef;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/novels")
public final class NovelController {
  final CanonicalTransferService transfers;
  final AccessKeyStore auditStore;
  final Clock clock;

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
    return NovelControllerCreateAction.create(this, requestBytes, idempotencyKey, authentication, servletRequest);
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
    return NovelControllerRevisionAction.revision(this, novelId, revisionId, authentication);
  }

  static Map<String, Object> head(Ids.NovelId novelId, RevisionRef head) {
    return Map.of(
        "head_hash", head.contentHash(),
        "head_revision_id", head.revisionId().value(),
        "head_sequence", head.sequence(),
        "novel_id", novelId.value(),
        "schema_version", CanonicalRevision.SCHEMA_VERSION
    );
  }
}
