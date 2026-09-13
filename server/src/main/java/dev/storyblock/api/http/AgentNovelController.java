package dev.storyblock.api.http;

import dev.storyblock.application.AgentNovelRegistrationService;
import dev.storyblock.security.AccessKeyStore;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
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
    static final Set<String> REQUEST_FIELDS = Set.of(
            "chapters", "created_at", "expected_han_characters", "language",
            "main_characters", "novel_id", "title"
    );

    final AgentNovelRegistrationService registrations;
    final AccessKeyStore auditStore;
    final Clock clock;

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
        return AgentNovelControllerRegisterAction.register(this, requestBytes, idempotencyKey, authentication, servletRequest);
    }
}
