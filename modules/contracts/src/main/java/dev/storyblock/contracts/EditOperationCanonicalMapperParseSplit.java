package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseSplit {
    static EditOperation parseSplit(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                payload,
                Set.of("block", "split_after_grapheme", "new_blocks", "provenance_mapping"),
                "split_block.payload"
        );
        return new EditOperation.SplitBlock(
                context,
                EditOperationCanonicalMapperParseRange.parseRange(EditOperationCanonicalMapper.object(payload.get("block"), "block")),
                EditOperationCanonicalMapperExactInt.exactInt(payload.get("split_after_grapheme"), "split_after_grapheme"),
                EditOperationCanonicalMapperArray.array(payload.get("new_blocks"), "new_blocks").stream()
                        .map(value -> EditOperationCanonicalMapperParseDraft.parseDraft(EditOperationCanonicalMapper.object(value, "block")))
                        .toList()
        );
    }
}
