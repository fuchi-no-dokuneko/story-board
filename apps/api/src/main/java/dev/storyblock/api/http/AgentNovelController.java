package dev.storyblock.api.http;

import dev.storyblock.application.AgentNovelRegistration;
import dev.storyblock.application.AgentNovelRegistrationService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyStore;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/agent/novels")
public final class AgentNovelController {
    private static final Set<String> REQUEST_FIELDS = Set.of(
            "chapters", "created_at", "expected_han_characters", "language",
            "main_characters", "novel_id", "title", "tnt_cannon_count",
            "zombie_count"
    );

    private final AgentNovelRegistrationService registrations;
    private final AccessKeyStore auditStore;
    private final Clock clock;

    public AgentNovelController(
            AgentNovelRegistrationService registrations,
            AccessKeyStore auditStore,
            Clock clock
    ) {
        this.registrations = java.util.Objects.requireNonNull(
                registrations, "registrations"
        );
        this.auditStore = java.util.Objects.requireNonNull(auditStore, "auditStore");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> register(
            @RequestBody byte[] requestBytes,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY)
            String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "agent novel registration"
        );
        StrictJsonRequest.requireKeys(
                request, REQUEST_FIELDS, "agent novel registration"
        );
        Ids.NovelId novelId = new Ids.NovelId(StrictJsonRequest.string(
                request, "novel_id", "agent novel registration"
        ));
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
        AgentNovelRegistrationService.Result result = registrations.register(
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

        Instant now = Instant.now(clock);
        AuditContext audit = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        auditStore.appendAuditEvent(AuditEvent.create(
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
