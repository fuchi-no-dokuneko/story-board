package dev.storyblock.api.http;

import dev.storyblock.detector.DetectorRun;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderRange;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class DetectorControllerDetectAction {
    static ResponseEntity<Map<String, Object>> detect(DetectorController self, String novelId, byte[] requestBytes, String ifMatch, Authentication authentication)  {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "detector request"
        );
        DetectorControllerRequireRequestFields.requireRequestFields(request);

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
                DetectorControllerOptionalBlockId.optionalBlockId(request.get("from_block_id"), "from_block_id"),
                DetectorControllerOptionalBlockId.optionalBlockId(request.get("to_block_id"), "to_block_id")
        );
        DetectorRun run = self.detectors.detect(
                requestedNovel,
                new Ids.RevisionId(StrictJsonRequest.string(
                        request, "revision_id", "detector request"
                )),
                expectedHash,
                range
        );
        self.telemetry.recordDetectorFindings(run.findings());
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, '"' + run.revisionHash() + '"')
                .body(run.canonicalValue());
    }
}
