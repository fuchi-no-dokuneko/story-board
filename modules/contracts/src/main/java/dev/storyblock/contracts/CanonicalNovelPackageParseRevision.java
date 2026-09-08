package dev.storyblock.contracts;

import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.RevisionEntry;
import static dev.storyblock.contracts.CanonicalNovelPackage.REVISION_FIELDS;

final class CanonicalNovelPackageParseRevision {
    static RevisionEntry parseRevision(Map<String, Object> value) {
        CanonicalNovelPackageRequireKeys.requireKeys(value, REVISION_FIELDS, "revision");
        Map<String, Object> document = CanonicalNovelPackage.object(value.get("document"), "revision.document");
        try {
            return new RevisionEntry(
                    CanonicalNovelPackageExactLong.exactLong(value.get("sequence"), "revision.sequence"),
                    CanonicalRevision.parseEnvelope(CanonicalJson.bytes(document))
            );
        } catch (IllegalArgumentException failure) {
            throw new CanonicalPackageException("Canonical package revision is invalid", failure);
        }
    }
}
