package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseInsert {
    static EditOperation parseInsert(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(payload, Set.of("insertion_point", "blocks"), "insert_blocks.payload");
        return new EditOperation.InsertBlocks(
                context,
                EditOperationCanonicalMapperParseInsertionPoint.parseInsertionPoint(EditOperationCanonicalMapper.object(payload.get("insertion_point"), "insertion_point")),
                EditOperationCanonicalMapperArray.array(payload.get("blocks"), "blocks").stream()
                        .map(value -> EditOperationCanonicalMapperParseDraft.parseDraft(EditOperationCanonicalMapper.object(value, "block")))
                        .toList()
        );
    }
}
