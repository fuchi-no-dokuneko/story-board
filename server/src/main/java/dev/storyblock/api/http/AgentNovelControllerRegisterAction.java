package dev.storyblock.api.http;

import dev.storyblock.application.AgentNovelRegistrationService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.*;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class AgentNovelControllerRegisterAction {
  static ResponseEntity<Map<String, Object>> register(AgentNovelController self, byte[] requestBytes, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
    Map<String, Object> request = StrictJsonRequest.parseObject(
        requestBytes, "agent novel registration"
    );
    StrictJsonRequest.requireKeys(
        request, AgentNovelController.REQUEST_FIELDS, "agent novel registration"
    );
    Ids.NovelId novelId = new Ids.NovelId(StrictJsonRequest.string(
        request, "novel_id", "agent novel registration"
    ));
    AccessPrincipalSupport.requireNovel(authentication, novelId);
    AgentNovelRegistrationService.Result result = self.registrations.register(
        AgentRegistrationInput.parse(request, novelId),
        idempotencyKey
    );

    Instant now = Instant.now(self.clock);
    AuditContext audit = AccessPrincipalSupport.auditContext(
        authentication, servletRequest, now
    );
    self.auditStore.appendAuditEvent(AuditEvent.create(
        audit,
        novelId,
        AuditAction.CANONICAL_IMPORT,
        result.novel().headRevisionId().value(),
        null,
        result.novel().headRevisionId(),
        result.importResult().idempotentReplay()
            ? AuditResult.IDEMPOTENT : AuditResult.SUCCEEDED,
        null,
        result.novel().headHash()
    ));

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("idempotent_replay", result.importResult().idempotentReplay());
    response.put("novel", AdminNovelController.entry(result.novel()));
    response.put("schema_version", "agent-novel-registration-1.0.0");
    HttpStatus status = result.importResult().idempotentReplay()
        ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status)
        .location(URI.create("/v1/admin/novels/" + novelId.value()))
        .eTag(result.novel().headHash())
        .body(response);
  }
}
