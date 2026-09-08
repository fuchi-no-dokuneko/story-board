package dev.storyblock.contracts;

import java.util.Map;
import static dev.storyblock.contracts.CanonicalNovelPackage.OperationEntry;

final class CanonicalNovelPackageOperationMap {
    static Map<String, Object> operationMap(OperationEntry value) {
        return Map.of(
                "sequence", value.sequence(),
                "operation_hash", value.operationHash(),
                "operation", EditOperationCanonicalMapper.toCanonical(value.operation()),
                "result_revision_id", value.resultRevisionId().value(),
                "result_hash", value.resultHash(),
                "committed_at", value.committedAt().toString()
        );
    }
}
