package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperFromCanonicalFactory {
    static EditOperation fromCanonical(Map<String, Object> envelope)  {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                envelope,
                Set.of(
                        "operation_id", "idempotency_key", "novel_id", "base_revision_id",
                        "expected_head_hash", "type", "payload"
                ),
                "operation"
        );
        EditContext context = new EditContext(
                new Ids.OperationId(EditOperationCanonicalMapperString.string(envelope, "operation_id", "operation")),
                EditOperationCanonicalMapperString.string(envelope, "idempotency_key", "operation"),
                new Ids.NovelId(EditOperationCanonicalMapperString.string(envelope, "novel_id", "operation")),
                new Ids.RevisionId(EditOperationCanonicalMapperString.string(envelope, "base_revision_id", "operation")),
                EditOperationCanonicalMapperString.string(envelope, "expected_head_hash", "operation")
        );
        String type = EditOperationCanonicalMapperString.string(envelope, "type", "operation");
        Map<String, Object> payload = EditOperationCanonicalMapper.object(envelope.get("payload"), "operation.payload");
        return switch (type) {
            case "insert_blocks" -> EditOperationCanonicalMapperParseInsert.parseInsert(context, payload);
            case "replace_block_range" -> EditOperationCanonicalMapperParseReplace.parseReplace(context, payload);
            case "delete_block_range" -> EditOperationCanonicalMapperParseDelete.parseDelete(context, payload);
            case "split_block" -> EditOperationCanonicalMapperParseSplit.parseSplit(context, payload);
            case "merge_blocks" -> EditOperationCanonicalMapperParseMerge.parseMerge(context, payload);
            case "extend_block" -> EditOperationCanonicalMapperParseExtend.parseExtend(context, payload);
            case "move_block_range" -> EditOperationCanonicalMapperParseMove.parseMove(context, payload);
            case "correct_block_meta" -> EditOperationCanonicalMapperParseCorrection.parseCorrection(context, payload);
            case "set_scene_initial_meta" -> EditOperationCanonicalMapperParseSceneSeed.parseSceneSeed(context, payload);
            case "restore_revision_content" -> EditOperationCanonicalMapperParseRestore.parseRestore(context, payload);
            default -> throw new IllegalArgumentException("Unsupported operation type: " + type);
        };
    }
}
