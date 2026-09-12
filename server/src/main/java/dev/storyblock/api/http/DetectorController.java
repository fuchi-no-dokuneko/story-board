package dev.storyblock.api.http;

import dev.storyblock.application.DetectorService;
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
    static final Set<String> REQUEST_FIELDS = Set.of(
            "revision_id", "revision_hash", "from_block_id", "to_block_id"
    );

    final DetectorService detectors;
    final StoryBlockTelemetry telemetry;

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
        return DetectorControllerDetectAction.detect(this, novelId, requestBytes, ifMatch, authentication);
    }

}
