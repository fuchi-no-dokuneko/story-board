package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MonitorOutput;
import dev.storyblock.monitor.MonitorSubmissionResult;
import dev.storyblock.security.AuditContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;

final class MonitorControllerSubmitAction {
  static ResponseEntity<Map<String, Object>> submit(MonitorController self, String novelId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication, HttpServletRequest httpRequest)  {
    Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
    AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
    Map<String, Object> request = StrictJsonRequest.parseObject(
        requestBytes, "monitor submission"
    );
    StrictJsonRequest.requireKeys(
        request, MonitorController.SUBMISSION_FIELDS, "monitor submission"
    );
    String expectedHash = MonitorControllerMatchedRevisionHash.matchedRevisionHash(
        request, ifMatch, "monitor submission"
    );
    Instant submittedAt = self.clock.instant();
    AuditContext auditContext = AccessPrincipalSupport.auditContext(
        authentication, httpRequest, submittedAt
    );
    MonitorSubmissionResult result = self.monitors.submit(
        requestedNovel,
        MonitorControllerRevisionId.revisionId(request, "monitor submission"),
        expectedHash,
        MonitorControllerBlockId.blockId(request, "target_block_id", "monitor submission"),
        MonitorControllerExactInt.exactInt(request.get("neighbor_count"), "monitor submission.neighbor_count"),
        StrictJsonRequest.string(request, "rule_version", "monitor submission"),
        StrictJsonRequest.uniqueStrings(
            request, "affected_block_ids", "monitor submission"
        ).stream().map(Ids.BlockId::new).toList(),
        MonitorOutput.fromCanonical(StrictJsonRequest.object(
            request.get("output"), "monitor submission.output"
        )),
        idempotencyKey,
        auditContext
    );
    HttpStatus status = result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status)
        .header(HttpHeaders.ETAG, MonitorControllerQuote.quote(result.status().run().revisionHash()))
        .header(
            HttpHeaders.LOCATION,
            "/v1/novels/" + novelId + "/monitor-runs/"
                + result.status().run().runId().value()
        )
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(result.canonicalValue());
  }
}
