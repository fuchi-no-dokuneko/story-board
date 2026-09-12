package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseReplace {
    static EditOperation parseReplace(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(payload, Set.of("range", "new_blocks"), "replace_block_range.payload");
        return new EditOperation.ReplaceBlockRange(
                context,
                EditOperationCanonicalMapperParseRange.parseRange(EditOperationCanonicalMapper.object(payload.get("range"), "range")),
                EditOperationCanonicalMapperArray.array(payload.get("new_blocks"), "new_blocks").stream()
                        .map(value -> EditOperationCanonicalMapperParseDraft.parseDraft(EditOperationCanonicalMapper.object(value, "block")))
                        .toList()
        );
    }
}
