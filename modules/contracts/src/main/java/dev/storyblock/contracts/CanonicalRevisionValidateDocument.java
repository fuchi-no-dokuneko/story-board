package dev.storyblock.contracts;

import dev.storyblock.domain.StableIds;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import static dev.storyblock.contracts.CanonicalRevisionFields.ROOT_OPTIONAL;
import static dev.storyblock.contracts.CanonicalRevision.SCHEMA_VERSION;
import static dev.storyblock.contracts.CanonicalRevisionFields.ROOT_REQUIRED;

final class CanonicalRevisionValidateDocument {
    static void validateDocument(Map<String, Object> document) {
        CanonicalRevisionValidateKeys.validateKeys(document, ROOT_REQUIRED, ROOT_OPTIONAL, "document");
        CanonicalRevisionRequireExactString.requireExactString(document, "schema_version", SCHEMA_VERSION, "document");
        StableIds.require(CanonicalRevisionRequireString.requireString(document, "novel_id", "document"), "nov");
        StableIds.require(CanonicalRevisionRequireString.requireString(document, "revision_id", "document"), "rev");

        Object parentRevision = document.get("parent_revision_id");
        if (parentRevision != null) {
            StableIds.require(CanonicalRevisionRequireString.requireString(document, "parent_revision_id", "document"), "rev");
        }

        String createdAt = CanonicalRevisionRequireString.requireString(document, "created_at", "document");
        try {
            Instant instant = Instant.parse(createdAt);
            if (!instant.toString().equals(createdAt)) {
                throw new IllegalArgumentException("document.created_at must use canonical UTC form");
            }
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("document.created_at must be an ISO-8601 instant", exception);
        }

        CanonicalRevisionValidateExtensions.validateExtensions(document.get("extensions"), "document.extensions");
        List<Object> chapters = CanonicalRevisionRequireList.requireList(document, "chapters", "document");
        for (int index = 0; index < chapters.size(); index++) {
            CanonicalRevisionValidateChapter.validateChapter(CanonicalRevision.requireMap(chapters.get(index), "chapter[" + index + "]"), index);
        }
    }
}
