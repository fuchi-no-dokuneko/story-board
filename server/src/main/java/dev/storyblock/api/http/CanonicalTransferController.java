package dev.storyblock.api.http;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.security.AccessKeyStore;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public final class CanonicalTransferController {
  public static final String ARTIFACT_CODEC_HEADER = "X-Artifact-Codec";

  final CanonicalTransferService transfers;
  final StyleAnalysisService analyses;
  final AccessKeyStore securityStore;
  final Clock clock;

  public CanonicalTransferController(
      CanonicalTransferService transfers,
      StyleAnalysisService analyses,
      AccessKeyStore securityStore,
      Clock clock
  ) {
    this.transfers = java.util.Objects.requireNonNull(transfers, "transfers");
    this.analyses = java.util.Objects.requireNonNull(analyses, "analyses");
    this.securityStore = java.util.Objects.requireNonNull(
        securityStore, "securityStore"
    );
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
  }

  @PostMapping("/imports")
  ResponseEntity<Map<String, Object>> importNovel(
      @RequestBody byte[] requestBytes,
      @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
      Authentication authentication,
      HttpServletRequest servletRequest
  ) {
    return CanonicalTransferControllerImportNovelAction.importNovel(this, requestBytes, idempotencyKey, authentication, servletRequest);
  }

  @PostMapping("/novels/{novelId}/exports")
  ResponseEntity<Map<String, Object>> startExport(
      @PathVariable String novelId,
      @RequestBody byte[] requestBytes,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
      Authentication authentication,
      HttpServletRequest servletRequest
  ) {
    return CanonicalTransferControllerStartExportAction.startExport(this, novelId, requestBytes, ifMatch, idempotencyKey, authentication, servletRequest);
  }

  @GetMapping("/jobs/{jobId}")
  ResponseEntity<Map<String, Object>> getJob(@PathVariable String jobId) {
    return CanonicalTransferControllerGetJobAction.getJob(this, jobId);
  }

  @GetMapping("/artifacts/{artifactId}")
  ResponseEntity<byte[]> getArtifact(@PathVariable String artifactId) {
    return CanonicalTransferControllerGetArtifactAction.getArtifact(this, artifactId);
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> object(Object value, String path) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new CanonicalPackageException(path + " must be an object");
    }
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) {
        throw new CanonicalPackageException(path + " contains a non-string key");
      }
    }
    return (Map<String, Object>) map;
  }

}
