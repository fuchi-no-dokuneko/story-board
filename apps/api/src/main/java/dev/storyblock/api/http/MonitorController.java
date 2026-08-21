package dev.storyblock.api.http;

import dev.storyblock.application.MonitorService;
import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MonitorOutput;
import dev.storyblock.monitor.MonitorPacket;
import dev.storyblock.monitor.MonitorRunStatus;
import dev.storyblock.monitor.MonitorSubmissionResult;
import dev.storyblock.security.AuditContext;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private static final Set<String> PACKET_FIELDS = Set.of(
            "revision_id", "revision_hash", "target_block_id", "neighbor_count"
    );
    private static final Set<String> SUBMISSION_FIELDS = Set.of(
            "revision_id", "revision_hash", "target_block_id", "neighbor_count",
            "rule_version", "affected_block_ids", "output"
    );

    private final MonitorService monitors;
    private final Clock clock;

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
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "monitor packet request"
        );
        StrictJsonRequest.requireKeys(
                request, PACKET_FIELDS, "monitor packet request"
        );
        String expectedHash = matchedRevisionHash(
                request, ifMatch, "monitor packet request"
        );
        MonitorPacket packet = monitors.packet(
                requestedNovel,
                revisionId(request, "monitor packet request"),
                expectedHash,
                blockId(request, "target_block_id", "monitor packet request"),
                exactInt(request.get("neighbor_count"), "monitor packet request.neighbor_count")
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, quote(packet.revisionHash()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(packet.canonicalValue());
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
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "monitor submission"
        );
        StrictJsonRequest.requireKeys(
                request, SUBMISSION_FIELDS, "monitor submission"
        );
        String expectedHash = matchedRevisionHash(
                request, ifMatch, "monitor submission"
        );
        Instant submittedAt = clock.instant();
        AuditContext auditContext = AccessPrincipalSupport.auditContext(
                authentication, httpRequest, submittedAt
        );
        MonitorSubmissionResult result = monitors.submit(
                requestedNovel,
                revisionId(request, "monitor submission"),
                expectedHash,
                blockId(request, "target_block_id", "monitor submission"),
                exactInt(request.get("neighbor_count"), "monitor submission.neighbor_count"),
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
                .header(HttpHeaders.ETAG, quote(result.status().run().revisionHash()))
                .header(
                        HttpHeaders.LOCATION,
                        "/v1/novels/" + novelId + "/monitor-runs/"
                                + result.status().run().runId().value()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.canonicalValue());
    }

    @GetMapping("/novels/{novelId}/monitor-runs/{runId}")
    ResponseEntity<Map<String, Object>> status(
            @PathVariable String novelId,
            @PathVariable String runId,
            Authentication authentication
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        MonitorRunStatus status = monitors.getStatus(
                requestedNovel, new Ids.MonitorRunId(runId)
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, quote(status.run().revisionHash()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(status.canonicalValue());
    }

    private static Ids.RevisionId revisionId(
            Map<String, Object> request,
            String path
    ) {
        return new Ids.RevisionId(StrictJsonRequest.string(
                request, "revision_id", path
        ));
    }

    private static Ids.BlockId blockId(
            Map<String, Object> request,
            String field,
            String path
    ) {
        return new Ids.BlockId(StrictJsonRequest.string(request, field, path));
    }

    private static String matchedRevisionHash(
            Map<String, Object> request,
            String ifMatch,
            String path
    ) {
        String requestHash = StrictJsonRequest.string(
                request, "revision_hash", path
        );
        String expectedHash = StrictJsonRequest.unquoteEtag(ifMatch);
        if (!requestHash.equals(expectedHash)) {
            throw new IllegalArgumentException(
                    path + ".revision_hash must match If-Match"
            );
        }
        return expectedHash;
    }

    private static int exactInt(Object value, String path) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(path + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(path + " must be an exact integer", failure);
        }
    }

    private static String quote(String hash) {
        return '"' + hash + '"';
    }
}
