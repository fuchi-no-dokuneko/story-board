package dev.storyblock.api.http;

import dev.storyblock.application.CommitService;
import dev.storyblock.security.AccessKeyStore;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public final class CommitController {
    final CommitService commits;
    final AccessKeyStore securityStore;
    final Clock clock;
    final RecentReadGuard reads;

    public CommitController(
            CommitService commits,
            AccessKeyStore securityStore,
            Clock clock, RecentReadGuard reads
    ) {
        this.commits = java.util.Objects.requireNonNull(commits, "commits");
        this.securityStore = java.util.Objects.requireNonNull(
                securityStore, "securityStore"
        );
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.reads = reads;
    }

    @PostMapping("/novels/{novelId}/commits")
    ResponseEntity<Map<String, Object>> commit(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return CommitControllerCommitAction.commit(this, novelId, requestBytes, ifMatch, idempotencyKey, authentication, servletRequest);
    }
}
