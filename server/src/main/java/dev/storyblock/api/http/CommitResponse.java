package dev.storyblock.api.http;
import dev.storyblock.security.*;
import dev.storyblock.storage.CommitResult;
import java.util.Map;
import org.springframework.http.*;

final class CommitResponse {
  static ResponseEntity<Map<String, Object>> create(CommitResult result) {
    HttpStatus status = result.idempotentReplay()
        ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status)
        .header(HttpHeaders.ETAG, "\"" + result.revision().contentHash() + "\"")
        .body(Map.of(
            "revision_id", result.revision().revisionId().value(),
            "sequence", result.revision().sequence(),
            "content_hash", result.revision().contentHash(),
            "operation_id", result.operationId().value(),
            "idempotent_replay", result.idempotentReplay()
        ));  }
}
