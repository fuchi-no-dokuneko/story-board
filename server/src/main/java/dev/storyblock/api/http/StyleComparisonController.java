package dev.storyblock.api.http;

import dev.storyblock.api.style.*;
import java.util.*;
import java.util.concurrent.Callable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public final class StyleComparisonController {
    private final StyleLibrary library;
    private final StyleComparisonService comparisons;
    public StyleComparisonController(StyleLibrary library, StyleComparisonService comparisons) {
        this.library = library; this.comparisons = comparisons;
    }

    @GetMapping("/v1/style/")
    ResponseEntity<Map<String, Object>> list() {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(library.list());
    }

    @PostMapping("/v1/novels/{novelId}/style-comparisons")
    Callable<ResponseEntity<Map<String, Object>>> compare(@PathVariable String novelId,
            @RequestBody byte[] bytes, @RequestHeader("If-Match") String ifMatch) {
        var request = StrictJsonRequest.parseObject(bytes, "style comparison");
        StyleAnalysisControllerRequireFields.requireFields(request, Set.of("style_ids"), Set.of("style_ids", "revision_id"), "style comparison");
        if (!(request.get("style_ids") instanceof List<?> raw)
                || raw.stream().anyMatch(value -> !(value instanceof String)))
            throw new IllegalArgumentException("style_ids must be a list of strings");
        var ids = raw.stream().map(String.class::cast).toList();
        String revision = request.containsKey("revision_id")
                ? StrictJsonRequest.string(request, "revision_id", "style comparison") : null;
        String hash = StrictJsonRequest.unquoteEtag(ifMatch);
        return () -> ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(comparisons.compare(novelId, revision, hash, ids));
    }
}
