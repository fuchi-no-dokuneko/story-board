package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.OperationEntry;
import static dev.storyblock.contracts.CanonicalPackageFields.OPERATION_FIELDS;

final class CanonicalNovelPackageParseOperation {
    static OperationEntry parseOperation(Map<String, Object> value) {
        CanonicalNovelPackageRequireKeys.requireKeys(value, OPERATION_FIELDS, "operation entry");
        try {
            return new OperationEntry(
                    CanonicalNovelPackageExactLong.exactLong(value.get("sequence"), "operation.sequence"),
                    CanonicalNovelPackageString.string(value, "operation_hash", "operation entry"),
                    EditOperationCanonicalMapper.fromCanonical(
                            CanonicalPackageObject.object(value.get("operation"), "operation")
                    ),
                    new Ids.RevisionId(CanonicalNovelPackageString.string(
                            value, "result_revision_id", "operation entry"
                    )),
                    CanonicalNovelPackageString.string(value, "result_hash", "operation entry"),
                    CanonicalNovelPackageInstant.instant(value, "committed_at", "operation entry")
            );
        } catch (IllegalArgumentException failure) {
            throw new CanonicalPackageException("Canonical package operation is invalid", failure);
        }
    }
}
