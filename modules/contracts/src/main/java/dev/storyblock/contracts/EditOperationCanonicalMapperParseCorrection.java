package dev.storyblock.contracts;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseCorrection {
    static EditOperation parseCorrection(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                payload,
                Set.of("scene_id", "block", "corrected_meta"),
                "correct_block_meta.payload"
        );
        return new EditOperation.CorrectBlockMeta(
                context,
                new Ids.SceneId(EditOperationCanonicalMapperString.string(payload, "scene_id", "correct_block_meta.payload")),
                EditOperationCanonicalMapperParseBlockReference.parseBlockReference(EditOperationCanonicalMapper.object(payload.get("block"), "block")),
                new BlockMetadata(EditOperationCanonicalMapper.object(payload.get("corrected_meta"), "corrected_meta"))
        );
    }
}
