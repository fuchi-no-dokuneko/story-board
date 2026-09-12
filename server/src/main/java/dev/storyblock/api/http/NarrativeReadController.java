package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/novels/{novelId}")
public final class NarrativeReadController {
    private final SqliteRevisionStore store;
    public NarrativeReadController(SqliteRevisionStore store) { this.store = store; }

    @GetMapping("/chapters")
    Map<String, Object> chapters(@PathVariable String novelId, @RequestParam String revision_id) {
        var revision = store.getRevision(new Ids.NovelId(novelId), new Ids.RevisionId(revision_id));
        return NarrativeReadViews.chapters(revision);
    }

    @GetMapping("/scenes")
    Map<String, Object> scenes(@PathVariable String novelId, @RequestParam String revision_id,
            @RequestParam(defaultValue = "0") int chapter_start,
            @RequestParam(required = false) Integer chapter_end) {
        var revision = store.getRevision(new Ids.NovelId(novelId), new Ids.RevisionId(revision_id));
        return NarrativeReadViews.scenes(revision, chapter_start, chapter_end);
    }

    @GetMapping("/scenes/{sceneId}/blocks")
    Map<String, Object> blocks(@PathVariable String novelId, @PathVariable String sceneId,
            @RequestParam String revision_id, @RequestParam int start, @RequestParam int end) {
        var revision = store.getRevision(new Ids.NovelId(novelId), new Ids.RevisionId(revision_id));
        return BlockSliceView.create(revision, new Ids.SceneId(sceneId), start, end);
    }
}
