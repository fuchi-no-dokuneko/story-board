package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.RevisionManifest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NarrativeCanonicalMapper {
    private NarrativeCanonicalMapper() {
    }

    public static CanonicalRevision toCanonical(RevisionManifest manifest) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schema_version", CanonicalRevision.SCHEMA_VERSION);
        document.put("novel_id", manifest.novel().id().value());
        document.put("revision_id", manifest.id().value());
        document.put("parent_revision_id", manifest.parentId() == null ? null : manifest.parentId().value());
        document.put("chapters", manifest.novel().chapters().stream()
                .map(NarrativeCanonicalMapperChapterToCanonical::chapterToCanonical)
                .toList());
        document.put("created_at", manifest.createdAt().toString());
        NarrativeCanonicalMapperPutExtensions.putExtensions(document, manifest.novel().extensions());
        return CanonicalRevision.of(document);
    }

    public static RevisionManifest fromCanonical(CanonicalRevision canonical) {
        Map<String, Object> document = canonical.canonicalContent();
        List<NarrativeChapter> chapters = NarrativeCanonicalMapperRequireList.requireList(document.get("chapters"), "chapters").stream()
                .map(value -> NarrativeCanonicalMapperChapterFromCanonical.chapterFromCanonical(requireMap(value, "chapter")))
                .toList();
        Ids.RevisionId parentId = document.get("parent_revision_id") == null
                ? null
                : new Ids.RevisionId(NarrativeCanonicalMapperRequireString.requireString(document, "parent_revision_id"));
        return new RevisionManifest(
                new Ids.RevisionId(NarrativeCanonicalMapperRequireString.requireString(document, "revision_id")),
                parentId,
                Instant.parse(NarrativeCanonicalMapperRequireString.requireString(document, "created_at")),
                new NarrativeNovel(
                        new Ids.NovelId(NarrativeCanonicalMapperRequireString.requireString(document, "novel_id")),
                        chapters,
                        NarrativeCanonicalMapperOptionalMap.optionalMap(document.get("extensions"), "extensions")
                )
        );
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> requireMap(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }

}
