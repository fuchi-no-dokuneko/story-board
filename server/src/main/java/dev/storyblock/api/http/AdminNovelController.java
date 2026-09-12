package dev.storyblock.api.http;

import dev.storyblock.application.NovelCatalogEntry;
import dev.storyblock.application.NovelCatalogService;
import java.util.LinkedHashMap;
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

    final NovelCatalogService catalog;

    public AdminNovelController(NovelCatalogService catalog) {
        this.catalog = java.util.Objects.requireNonNull(catalog, "catalog");
    }

    @GetMapping
    ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String q
    ) {
        return AdminNovelControllerListAction.list(this, page, size, q);
    }

    @GetMapping("/{novelId}")
    ResponseEntity<Map<String, Object>> read(@PathVariable String novelId) {
        return AdminNovelControllerReadAction.read(this, novelId);
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
