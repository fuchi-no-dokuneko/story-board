package dev.storyblock.api.http;

import dev.storyblock.application.DetectorService;
import dev.storyblock.detector.DetectorRun;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderRange;
import java.util.Map;
import java.util.Set;
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
public final class DetectorController {
    private static final Set<String> REQUEST_FIELDS = Set.of(
            "revision_id", "revision_hash", "from_block_id", "to_block_id"
    );

    private final DetectorService detectors;
    private final StoryBlockTelemetry telemetry;

    public DetectorController(
            DetectorService detectors,
            StoryBlockTelemetry telemetry
    ) {
        this.detectors = java.util.Objects.requireNonNull(detectors, "detectors");
        this.telemetry = java.util.Objects.requireNonNull(telemetry, "telemetry");
    }

    @PostMapping("/novels/{novelId}/detector-runs")
    ResponseEntity<Map<String, Object>> detect(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            Authentication authentication
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "detector request"
        );
        requireRequestFields(request);

        String requestHash = StrictJsonRequest.string(
                request, "revision_hash", "detector request"
        );
        String expectedHash = StrictJsonRequest.unquoteEtag(ifMatch);
        if (!requestHash.equals(expectedHash)) {
            throw new IllegalArgumentException(
                    "detector request.revision_hash must match If-Match"
            );
        }
        RenderRange range = new RenderRange(
                optionalBlockId(request.get("from_block_id"), "from_block_id"),
                optionalBlockId(request.get("to_block_id"), "to_block_id")
        );
        DetectorRun run = detectors.detect(
                requestedNovel,
                new Ids.RevisionId(StrictJsonRequest.string(
                        request, "revision_id", "detector request"
                )),
                expectedHash,
                range
        );
        telemetry.recordDetectorFindings(run.findings());
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, '"' + run.revisionHash() + '"')
                .body(run.canonicalValue());
    }

    private static void requireRequestFields(Map<String, Object> request) {
        for (String required : Set.of("revision_id", "revision_hash")) {
            if (!request.containsKey(required)) {
                throw new IllegalArgumentException(
                        "detector request is missing " + required
                );
            }
        }
        for (String field : request.keySet()) {
            if (!REQUEST_FIELDS.contains(field)) {
                throw new IllegalArgumentException(
                        "detector request contains unknown field " + field
                );
            }
        }
    }

    private static Ids.BlockId optionalBlockId(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String id)) {
            throw new IllegalArgumentException(
                    "detector request." + field + " must be a string or null"
            );
        }
        return new Ids.BlockId(id);
    }
}
