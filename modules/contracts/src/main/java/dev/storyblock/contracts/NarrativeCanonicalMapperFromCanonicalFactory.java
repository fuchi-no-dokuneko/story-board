package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.RevisionManifest;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class NarrativeCanonicalMapperFromCanonicalFactory {
    static RevisionManifest fromCanonical(CanonicalRevision canonical)  {
        Map<String, Object> document = canonical.canonicalContent();
        List<NarrativeChapter> chapters = NarrativeCanonicalMapperRequireList.requireList(document.get("chapters"), "chapters").stream()
                .map(value -> NarrativeCanonicalMapperChapterFromCanonical.chapterFromCanonical(NarrativeCanonicalMapper.requireMap(value, "chapter")))
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
}
