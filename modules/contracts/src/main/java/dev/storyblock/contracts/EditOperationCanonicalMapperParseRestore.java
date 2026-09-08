package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseRestore {
    static EditOperation parseRestore(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                payload,
                Set.of("restore_revision_id", "expected_restore_hash"),
                "restore_revision_content.payload"
        );
        return new EditOperation.RestoreRevisionContent(
                context,
                new Ids.RevisionId(EditOperationCanonicalMapperString.string(
                        payload, "restore_revision_id", "restore_revision_content.payload"
                )),
                EditOperationCanonicalMapperString.string(payload, "expected_restore_hash", "restore_revision_content.payload")
        );
    }
}
