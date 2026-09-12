package dev.storyblock.api.http;

import dev.storyblock.application.ImageUploadService;
import java.time.Clock;
import java.util.Map;
import org.springframework.http.HttpHeaders;
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
    final ImageUploadService images;
    final Clock clock;

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
        return ImageControllerUploadAction.upload(this, novelId, content, ifMatch, idempotencyKey, authentication);
    }
}
