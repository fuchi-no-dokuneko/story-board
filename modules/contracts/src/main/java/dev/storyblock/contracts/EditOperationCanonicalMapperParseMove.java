package dev.storyblock.contracts;

import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseMove {
    static EditOperation parseMove(EditContext context, Map<String, Object> payload) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                payload,
                Set.of(
                        "range", "destination", "expected_source_boundary",
                        "expected_destination_boundary"
                ),
                "move_block_range.payload"
        );
        return new EditOperation.MoveBlockRange(
                context,
                EditOperationCanonicalMapperParseRange.parseRange(EditOperationCanonicalMapper.object(payload.get("range"), "range")),
                EditOperationCanonicalMapperParseInsertionPoint.parseInsertionPoint(EditOperationCanonicalMapper.object(payload.get("destination"), "destination")),
                EditOperationCanonicalMapperParseBoundary.parseBoundary(EditOperationCanonicalMapper.object(payload.get("expected_source_boundary"), "source_boundary")),
                EditOperationCanonicalMapperParseBoundary.parseBoundary(EditOperationCanonicalMapper.object(
                        payload.get("expected_destination_boundary"), "destination_boundary"
                ))
        );
    }
}
