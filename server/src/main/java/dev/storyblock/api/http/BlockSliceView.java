package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredRevision;
import java.util.*;

final class BlockSliceView {
    static Map<String, Object> create(StoredRevision revision, Ids.SceneId sceneId, int start, int end) {
        var scene = revision.manifest().requireScene(sceneId);
        NarrativeReadViews.slice(start, end, scene.blocks().size());
        var result = NarrativeReadViews.envelope(revision);
        var items = new ArrayList<Map<String, Object>>();
        for (int i = start; i < end; i++) {
            var block = scene.blocks().get(i);
            var value = new LinkedHashMap<String, Object>();
            value.put("index", i);
            value.put("block_id", block.id().value());
            value.put("block_version_id", block.versionId().value());
            value.put("order_key", block.orderKey().value());
            value.put("text", block.text());
            value.put("meta", block.metadata().fields());
            value.put("extensions", block.extensions());
            items.add(value);
        }
        result.put("scene_id", sceneId.value());
        result.put("chapter_id", scene.chapterId().value());
        result.put("block_count", scene.blocks().size());
        result.put("slice", Map.of("start", start, "end", end));
        result.put("blocks", items);
        return result;
    }
}
