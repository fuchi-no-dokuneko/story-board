package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import java.util.LinkedHashMap;
import java.util.Map;

final class EditOperationCanonicalMapperContext {
    static Map<String, Object> context(EditContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operation_id", context.operationId().value());
        result.put("idempotency_key", context.idempotencyKey());
        result.put("novel_id", context.novelId().value());
        result.put("base_revision_id", context.baseRevisionId().value());
        result.put("expected_head_hash", context.expectedHeadHash());
        return result;
    }
}
