package dev.storyblock.api.http;

import dev.storyblock.application.ImageUploadService;
import dev.storyblock.domain.Ids;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class ImageControllerUploadAction {
    static ResponseEntity<Map<String, Object>> upload(ImageController self, String novelId, byte[] content, String ifMatch, String idempotencyKey, Authentication authentication)  {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        ImageUploadService.Result result = self.images.upload(
                requestedNovel,
                StrictJsonRequest.unquoteEtag(ifMatch),
                idempotencyKey,
                content,
                Instant.now(self.clock)
        );
        String artifactUri = "/v1/artifacts/" + result.artifact().artifactId().value();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schema_version", "image-upload-1.0.0");
        body.put("artifact_id", result.artifact().artifactId().value());
        body.put("artifact_uri", artifactUri);
        body.put("content_hash", result.artifact().contentHash());
        body.put("media_type", result.artifact().mediaType());
        body.put("width_px", result.widthPixels());
        body.put("height_px", result.heightPixels());
        body.put("idempotent_replay", result.idempotentReplay());
        HttpStatus status = result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .location(URI.create(artifactUri))
                .header(HttpHeaders.ETAG, '"' + result.artifact().contentHash() + '"')
                .body(Map.copyOf(body));
    }
}
