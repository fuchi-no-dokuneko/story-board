package dev.storyblock.api.http;

import dev.storyblock.domain.*;
import dev.storyblock.storage.StoredRevision;
import java.util.*;

final class NarrativeReadViews {
    static Map<String, Object> envelope(StoredRevision revision) {
        var value = new LinkedHashMap<String, Object>();
        value.put("novel_id", revision.manifest().novel().id().value());
        value.put("revision_id", revision.manifest().id().value());
        value.put("revision_hash", revision.contentHash());
        return value;
    }

    static Map<String, Object> chapters(StoredRevision revision) {
        var value = envelope(revision);
        var chapters = revision.manifest().novel().chapters();
        var items = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < chapters.size(); i++) {
            var chapter = chapters.get(i);
            items.add(Map.of("index", i, "chapter_id", chapter.id().value(),
                    "title", Objects.toString(chapter.title(), ""), "scene_count", chapter.scenes().size()));
        }
        value.put("chapters", items);
        value.put("chapter_count", chapters.size());
        return value;
    }

    static Map<String, Object> scenes(StoredRevision revision, int start, Integer requestedEnd) {
        var chapters = revision.manifest().novel().chapters();
        int end = requestedEnd == null ? chapters.size() : requestedEnd;
        slice(start, end, chapters.size());
        var value = envelope(revision);
        var items = new ArrayList<Map<String, Object>>();
        for (int c = start; c < end; c++) {
            var chapter = chapters.get(c);
            for (int s = 0; s < chapter.scenes().size(); s++) {
                var scene = chapter.scenes().get(s);
                items.add(Map.of("scene_id", scene.id().value(), "index", s,
                        "chapter_id", chapter.id().value(), "chapter_index", c,
                        "title", Objects.toString(scene.title(), ""), "block_count", scene.blocks().size(),
                        "block_slice", Map.of("start", 0, "end", scene.blocks().size()),
                        "total_text_graphemes", scene.blocks().stream()
                            .mapToLong(b -> UnicodeText.graphemeCount(b.text())).sum()));
            }
        }
        value.put("scenes", items);
        value.put("chapter_slice", Map.of("start", start, "end", end));
        return value;
    }

    static void slice(int start, int end, int size) {
        if (start < 0 || end < start || end > size)
            throw new IllegalArgumentException("Slice requires 0 <= start <= end <= " + size);
    }
}
