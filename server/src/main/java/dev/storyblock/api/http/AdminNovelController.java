package dev.storyblock.api.http;

import dev.storyblock.application.NovelCatalogEntry;
import dev.storyblock.application.NovelCatalogService;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/novels")
public final class AdminNovelController {
    public static final String SCHEMA_VERSION = "admin-novel-reader-1.0.0";

    private final NovelCatalogService catalog;

    public AdminNovelController(NovelCatalogService catalog) {
        this.catalog = java.util.Objects.requireNonNull(catalog, "catalog");
    }

    @GetMapping
    ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String q
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (q.codePointCount(0, q.length()) > 200) {
            throw new IllegalArgumentException("q cannot exceed 200 Unicode characters");
        }
        List<NovelCatalogEntry> matching = catalog.list(q);
        int from = Math.min(Math.multiplyExact(page, size), matching.size());
        int to = Math.min(from + size, matching.size());
        int totalPages = matching.isEmpty() ? 0 : (matching.size() + size - 1) / size;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", matching.subList(from, to).stream()
                .map(AdminNovelController::entry)
                .toList());
        response.put("page", page);
        response.put("schema_version", SCHEMA_VERSION);
        response.put("size", size);
        response.put("total", matching.size());
        response.put("total_pages", totalPages);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{novelId}")
    ResponseEntity<Map<String, Object>> read(@PathVariable String novelId) {
        Ids.NovelId id = new Ids.NovelId(novelId);
        NovelCatalogEntry novel = catalog.get(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("novel", entry(novel));
        response.put("revision", catalog.revision(id).envelope());
        response.put("schema_version", SCHEMA_VERSION);
        return ResponseEntity.ok().eTag(novel.headHash()).body(response);
    }

    static Map<String, Object> entry(NovelCatalogEntry value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent_write_registered", value.agentWriteRegistered());
        result.put("block_count", value.blockCount());
        result.put("chapter_count", value.chapterCount());
        result.put("han_character_count", value.hanCharacterCount());
        result.put("han_text_sha256", value.hanTextSha256());
        result.put("head_hash", value.headHash());
        result.put("head_revision_id", value.headRevisionId().value());
        result.put("head_sequence", value.headSequence());
        result.put("language", value.language());
        result.put("main_characters", value.mainCharacters());
        result.put("novel_id", value.novelId().value());
        result.put("scene_count", value.sceneCount());
        result.put("title", value.title());
        result.put("tnt_cannon_count", value.tntCannonCount());
        result.put("updated_at", value.updatedAt().toString());
        result.put("zombie_count", value.zombieCount());
        return result;
    }
}
