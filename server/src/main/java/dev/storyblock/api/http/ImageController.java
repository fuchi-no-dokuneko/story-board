package dev.storyblock.api.http;

import dev.storyblock.application.ImageUploadService;
import dev.storyblock.domain.Ids;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/novels/{novelId}/images")
public final class ImageController {
    private final ImageUploadService images;
    private final Clock clock;

    public ImageController(ImageUploadService images, Clock clock) {
        this.images = java.util.Objects.requireNonNull(images, "images");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping(consumes = {
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.APPLICATION_OCTET_STREAM_VALUE
    })
    ResponseEntity<Map<String, Object>> upload(
            @PathVariable String novelId,
            @RequestBody byte[] content,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY)
            String idempotencyKey,
            Authentication authentication
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        ImageUploadService.Result result = images.upload(
                requestedNovel,
                StrictJsonRequest.unquoteEtag(ifMatch),
                idempotencyKey,
                content,
                Instant.now(clock)
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
