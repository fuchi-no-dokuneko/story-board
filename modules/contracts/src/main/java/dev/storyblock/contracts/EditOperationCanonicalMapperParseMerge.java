package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseMerge {
    static EditOperation parseMerge(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                payload,
                Set.of("range", "new_block", "provenance_mapping"),
                "merge_blocks.payload"
        );
        return new EditOperation.MergeBlocks(
                context,
                EditOperationCanonicalMapperParseRange.parseRange(EditOperationCanonicalMapper.object(payload.get("range"), "range")),
                EditOperationCanonicalMapperParseDraft.parseDraft(EditOperationCanonicalMapper.object(payload.get("new_block"), "new_block"))
        );
    }
}
