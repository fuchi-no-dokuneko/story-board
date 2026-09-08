package dev.storyblock.api.http;

import dev.storyblock.application.AgentNovelRegistration;
import dev.storyblock.application.AgentNovelRegistrationService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.*;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    List<AgentNovelRegistration.Chapter> chapters = StrictJsonRequest.objects(
        request.get("chapters"), "agent novel registration.chapters"
    ).stream().map(chapter -> {
      StrictJsonRequest.requireKeys(
          chapter, Set.of("text", "title"), "agent novel registration chapter"
      );
      return new AgentNovelRegistration.Chapter(
          StrictJsonRequest.string(
              chapter, "title", "agent novel registration chapter"
          ),
          StrictJsonRequest.string(
              chapter, "text", "agent novel registration chapter"
          )
      );
    }).toList();
    AgentNovelRegistrationService.Result result = self.registrations.register(
        new AgentNovelRegistration(
            novelId,
            StrictJsonRequest.instant(
                request, "created_at", "agent novel registration"
            ),
            StrictJsonRequest.string(
                request, "title", "agent novel registration"
            ),
            StrictJsonRequest.string(
                request, "language", "agent novel registration"
            ),
            StrictJsonRequest.uniqueStrings(
                request, "main_characters", "agent novel registration"
            ),
            StrictJsonRequest.integer(
                request, "zombie_count", "agent novel registration"
            ),
            StrictJsonRequest.integer(
                request, "tnt_cannon_count", "agent novel registration"
            ),
            StrictJsonRequest.integer(
                request, "expected_han_characters", "agent novel registration"
            ),
            chapters
        ),
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
