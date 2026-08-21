package dev.storyblock.contracts;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.InsertionPoint;
import dev.storyblock.domain.SceneBoundaryContract;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EditOperationCanonicalMapper {
    private EditOperationCanonicalMapper() {
    }

    public static Map<String, Object> toCanonical(EditOperation operation) {
        Map<String, Object> envelope = context(operation.context());
        envelope.put("type", operation.type().canonicalName());
        envelope.put("payload", payload(operation));
        return Map.copyOf(envelope);
    }

    public static String hash(EditOperation operation) {
        return CanonicalJson.hash(toCanonical(operation));
    }

    private static Map<String, Object> context(EditContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operation_id", context.operationId().value());
        result.put("idempotency_key", context.idempotencyKey());
        result.put("novel_id", context.novelId().value());
        result.put("base_revision_id", context.baseRevisionId().value());
        result.put("expected_head_hash", context.expectedHeadHash());
        return result;
    }

    private static Map<String, Object> payload(EditOperation operation) {
        return switch (operation) {
            case EditOperation.InsertBlocks insert -> Map.of(
                    "insertion_point", insertionPoint(insert.insertionPoint()),
                    "blocks", insert.blocks().stream()
                            .map(EditOperationCanonicalMapper::draft)
                            .toList()
            );
            case EditOperation.ReplaceBlockRange replace -> Map.of(
                    "range", range(replace.range()),
                    "new_blocks", replace.newBlocks().stream()
                            .map(EditOperationCanonicalMapper::draft)
                            .toList()
            );
            case EditOperation.DeleteBlockRange delete -> Map.of("range", range(delete.range()));
            case EditOperation.SplitBlock split -> Map.of(
                    "block", range(split.block()),
                    "split_after_grapheme", split.splitAfterGrapheme(),
                    "new_blocks", split.newBlocks().stream()
                            .map(EditOperationCanonicalMapper::draft)
                            .toList(),
                    "provenance_mapping", provenance(split.provenanceMapping().sourceToResults())
            );
            case EditOperation.MergeBlocks merge -> Map.of(
                    "range", range(merge.range()),
                    "new_block", draft(merge.newBlock()),
                    "provenance_mapping", provenance(merge.provenanceMapping().sourceToResults())
            );
            case EditOperation.ExtendBlock extend -> Map.of(
                    "block", range(extend.block()),
                    "position", extend.position().name().toLowerCase(java.util.Locale.ROOT),
                    "replacement", draft(extend.replacement())
            );
            case EditOperation.MoveBlockRange move -> Map.of(
                    "range", range(move.range()),
                    "destination", insertionPoint(move.destination()),
                    "expected_source_boundary", boundary(move.expectedSourceBoundary()),
                    "expected_destination_boundary", boundary(move.expectedDestinationBoundary())
            );
            case EditOperation.CorrectBlockMeta correction -> Map.of(
                    "scene_id", correction.sceneId().value(),
                    "block", blockReference(correction.block()),
                    "corrected_meta", correction.correctedMetadata().fields()
            );
            case EditOperation.SetSceneInitialMeta sceneSeed -> Map.of(
                    "scene_id", sceneSeed.sceneId().value(),
                    "expected_boundary", boundary(sceneSeed.expectedBoundary()),
                    "initial_meta", sceneSeed.initialMeta().fields()
            );
            case EditOperation.RestoreRevisionContent restore -> Map.of(
                    "restore_revision_id", restore.restoreRevisionId().value(),
                    "expected_restore_hash", restore.expectedRestoreHash()
            );
        };
    }

    private static Map<String, Object> draft(BlockDraft draft) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", draft.id().value());
        result.put("text", draft.text());
        result.put("meta", draft.metadata().fields());
        if (!draft.extensions().isEmpty()) {
            result.put("extensions", draft.extensions());
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> range(BlockRangeGuard range) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_id", range.sceneId().value());
        result.put("first_block_id", range.firstBlockId().value());
        result.put("last_block_id", range.lastBlockId().value());
        result.put("expected_blocks", range.expectedBlocks().stream()
                .map(EditOperationCanonicalMapper::blockReference)
                .toList());
        result.put("expected_range_hash", range.expectedRangeHash());
        result.put(
                "expected_previous_block_id",
                range.expectedPreviousBlockId() == null ? null : range.expectedPreviousBlockId().value()
        );
        result.put(
                "expected_next_block_id",
                range.expectedNextBlockId() == null ? null : range.expectedNextBlockId().value()
        );
        return java.util.Collections.unmodifiableMap(result);
    }

    private static Map<String, Object> blockReference(BlockVersionRef reference) {
        return Map.of(
                "block_id", reference.blockId().value(),
                "block_version_id", reference.blockVersionId().value()
        );
    }

    private static Map<String, Object> insertionPoint(InsertionPoint point) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_id", point.sceneId().value());
        result.put("position", point.position().name().toLowerCase(java.util.Locale.ROOT));
        if (point.anchorBlockId() != null) {
            result.put("anchor_block_id", point.anchorBlockId().value());
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> boundary(SceneBoundaryContract boundary) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_id", boundary.sceneId().value());
        result.put("first_block_id", boundary.firstBlockId() == null
                ? null : boundary.firstBlockId().value());
        result.put("last_block_id", boundary.lastBlockId() == null
                ? null : boundary.lastBlockId().value());
        result.put("expected_sequence_hash", boundary.expectedSequenceHash());
        return java.util.Collections.unmodifiableMap(result);
    }

    private static List<Map<String, Object>> provenance(
            Map<BlockVersionRef, List<dev.storyblock.domain.Ids.BlockId>> mapping
    ) {
        return mapping.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "source", blockReference(entry.getKey()),
                        "result_block_ids", entry.getValue().stream()
                                .map(dev.storyblock.domain.Ids.BlockId::value)
                                .toList()
                ))
                .toList();
    }
}
