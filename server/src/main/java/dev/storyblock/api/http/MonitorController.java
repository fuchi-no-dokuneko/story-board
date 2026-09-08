package dev.storyblock.api.http;

import dev.storyblock.application.MonitorService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public final class MonitorController {
    static final Set<String> PACKET_FIELDS = Set.of(
            "revision_id", "revision_hash", "target_block_id", "neighbor_count"
    );
    static final Set<String> SUBMISSION_FIELDS = Set.of(
            "revision_id", "revision_hash", "target_block_id", "neighbor_count",
            "rule_version", "affected_block_ids", "output"
    );

    final MonitorService monitors;
    final Clock clock;

    public MonitorController(MonitorService monitors, Clock clock) {
        this.monitors = java.util.Objects.requireNonNull(monitors, "monitors");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping("/novels/{novelId}/monitor-packets")
    ResponseEntity<Map<String, Object>> packet(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            Authentication authentication
    ) {
        return MonitorControllerPacketAction.packet(this, novelId, requestBytes, ifMatch, authentication);
    }

    @PostMapping("/novels/{novelId}/monitor-runs")
    ResponseEntity<Map<String, Object>> submit(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return MonitorControllerSubmitAction.submit(this, novelId, requestBytes, ifMatch, idempotencyKey, authentication, httpRequest);
    }

    @GetMapping("/novels/{novelId}/monitor-runs/{runId}")
    ResponseEntity<Map<String, Object>> status(
            @PathVariable String novelId,
            @PathVariable String runId,
            Authentication authentication
    ) {
        return MonitorControllerStatusAction.status(this, novelId, runId, authentication);
    }

}
