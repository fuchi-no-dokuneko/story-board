package dev.storyblock.api.http;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class OpenApiDocumentController {
    private static final Resource DOCUMENT = new ClassPathResource(
            "openapi/storyblock-v1.yaml"
    );

    @GetMapping(value = "/v1/openapi.yaml", produces = "application/yaml")
    ResponseEntity<Resource> openApi() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(DOCUMENT);
    }
}
