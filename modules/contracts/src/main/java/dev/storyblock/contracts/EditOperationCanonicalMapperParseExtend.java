package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseExtend {
    static EditOperation parseExtend(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                payload,
                Set.of("block", "position", "replacement"),
                "extend_block.payload"
        );
        return new EditOperation.ExtendBlock(
                context,
                EditOperationCanonicalMapperParseRange.parseRange(EditOperationCanonicalMapper.object(payload.get("block"), "block")),
                EditOperation.ExtensionPosition.valueOf(
                        EditOperationCanonicalMapperString.string(payload, "position", "extend_block.payload")
                                .toUpperCase(java.util.Locale.ROOT)
                ),
                EditOperationCanonicalMapperParseDraft.parseDraft(EditOperationCanonicalMapper.object(payload.get("replacement"), "replacement"))
        );
    }
}
