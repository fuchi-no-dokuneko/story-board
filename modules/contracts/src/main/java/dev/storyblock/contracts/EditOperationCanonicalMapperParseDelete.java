package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseDelete {
    static EditOperation parseDelete(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(payload, Set.of("range"), "delete_block_range.payload");
        return new EditOperation.DeleteBlockRange(
                context,
                EditOperationCanonicalMapperParseRange.parseRange(EditOperationCanonicalMapper.object(payload.get("range"), "range"))
        );
    }
}
