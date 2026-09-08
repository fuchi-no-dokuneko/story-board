package dev.storyblock.contracts;

import dev.storyblock.domain.RevisionManifest;
import java.util.LinkedHashMap;
import java.util.Map;

final class NarrativeCanonicalMapperToCanonicalFactory {
    static CanonicalRevision toCanonical(RevisionManifest manifest)  {
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
}
