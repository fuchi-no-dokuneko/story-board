package dev.storyblock.contracts;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.InsertionPoint;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.SceneBoundaryContract;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static EditOperation fromCanonical(byte[] canonicalJson) {
        @SuppressWarnings("unchecked")
        Map<String, Object> value = CanonicalJson.mapper().readValue(canonicalJson, Map.class);
        return fromCanonical(value);
    }

    public static EditOperation fromCanonical(Map<String, Object> envelope) {
        requireKeys(
                envelope,
                Set.of(
                        "operation_id", "idempotency_key", "novel_id", "base_revision_id",
                        "expected_head_hash", "type", "payload"
                ),
                "operation"
        );
        EditContext context = new EditContext(
                new Ids.OperationId(string(envelope, "operation_id", "operation")),
                string(envelope, "idempotency_key", "operation"),
                new Ids.NovelId(string(envelope, "novel_id", "operation")),
                new Ids.RevisionId(string(envelope, "base_revision_id", "operation")),
                string(envelope, "expected_head_hash", "operation")
        );
        String type = string(envelope, "type", "operation");
        Map<String, Object> payload = object(envelope.get("payload"), "operation.payload");
        return switch (type) {
            case "insert_blocks" -> parseInsert(context, payload);
            case "replace_block_range" -> parseReplace(context, payload);
            case "delete_block_range" -> parseDelete(context, payload);
            case "split_block" -> parseSplit(context, payload);
            case "merge_blocks" -> parseMerge(context, payload);
            case "extend_block" -> parseExtend(context, payload);
            case "move_block_range" -> parseMove(context, payload);
            case "correct_block_meta" -> parseCorrection(context, payload);
            case "set_scene_initial_meta" -> parseSceneSeed(context, payload);
            case "restore_revision_content" -> parseRestore(context, payload);
            default -> throw new IllegalArgumentException("Unsupported operation type: " + type);
        };
    }

    private static EditOperation parseInsert(EditContext context, Map<String, Object> payload) {
        requireKeys(payload, Set.of("insertion_point", "blocks"), "insert_blocks.payload");
        return new EditOperation.InsertBlocks(
                context,
                parseInsertionPoint(object(payload.get("insertion_point"), "insertion_point")),
                array(payload.get("blocks"), "blocks").stream()
                        .map(value -> parseDraft(object(value, "block")))
                        .toList()
        );
    }

    private static EditOperation parseReplace(EditContext context, Map<String, Object> payload) {
        requireKeys(payload, Set.of("range", "new_blocks"), "replace_block_range.payload");
        return new EditOperation.ReplaceBlockRange(
                context,
                parseRange(object(payload.get("range"), "range")),
                array(payload.get("new_blocks"), "new_blocks").stream()
                        .map(value -> parseDraft(object(value, "block")))
                        .toList()
        );
    }

    private static EditOperation parseDelete(EditContext context, Map<String, Object> payload) {
        requireKeys(payload, Set.of("range"), "delete_block_range.payload");
        return new EditOperation.DeleteBlockRange(
                context,
                parseRange(object(payload.get("range"), "range"))
        );
    }

    private static EditOperation parseSplit(EditContext context, Map<String, Object> payload) {
        requireKeys(
                payload,
                Set.of("block", "split_after_grapheme", "new_blocks", "provenance_mapping"),
                "split_block.payload"
        );
        return new EditOperation.SplitBlock(
                context,
                parseRange(object(payload.get("block"), "block")),
                exactInt(payload.get("split_after_grapheme"), "split_after_grapheme"),
                array(payload.get("new_blocks"), "new_blocks").stream()
                        .map(value -> parseDraft(object(value, "block")))
                        .toList()
        );
    }

    private static EditOperation parseMerge(EditContext context, Map<String, Object> payload) {
        requireKeys(
                payload,
                Set.of("range", "new_block", "provenance_mapping"),
                "merge_blocks.payload"
        );
        return new EditOperation.MergeBlocks(
                context,
                parseRange(object(payload.get("range"), "range")),
                parseDraft(object(payload.get("new_block"), "new_block"))
        );
    }

    private static EditOperation parseExtend(EditContext context, Map<String, Object> payload) {
        requireKeys(
                payload,
                Set.of("block", "position", "replacement"),
                "extend_block.payload"
        );
        return new EditOperation.ExtendBlock(
                context,
                parseRange(object(payload.get("block"), "block")),
                EditOperation.ExtensionPosition.valueOf(
                        string(payload, "position", "extend_block.payload")
                                .toUpperCase(java.util.Locale.ROOT)
                ),
                parseDraft(object(payload.get("replacement"), "replacement"))
        );
    }

    private static EditOperation parseMove(EditContext context, Map<String, Object> payload) {
        requireKeys(
                payload,
                Set.of(
                        "range", "destination", "expected_source_boundary",
                        "expected_destination_boundary"
                ),
                "move_block_range.payload"
        );
        return new EditOperation.MoveBlockRange(
                context,
                parseRange(object(payload.get("range"), "range")),
                parseInsertionPoint(object(payload.get("destination"), "destination")),
                parseBoundary(object(payload.get("expected_source_boundary"), "source_boundary")),
                parseBoundary(object(
                        payload.get("expected_destination_boundary"), "destination_boundary"
                ))
        );
    }

    private static EditOperation parseCorrection(EditContext context, Map<String, Object> payload) {
        requireKeys(
                payload,
                Set.of("scene_id", "block", "corrected_meta"),
                "correct_block_meta.payload"
        );
        return new EditOperation.CorrectBlockMeta(
                context,
                new Ids.SceneId(string(payload, "scene_id", "correct_block_meta.payload")),
                parseBlockReference(object(payload.get("block"), "block")),
                new BlockMetadata(object(payload.get("corrected_meta"), "corrected_meta"))
        );
    }

    private static EditOperation parseSceneSeed(EditContext context, Map<String, Object> payload) {
        requireKeys(
                payload,
                Set.of("scene_id", "expected_boundary", "initial_meta"),
                "set_scene_initial_meta.payload"
        );
        return new EditOperation.SetSceneInitialMeta(
                context,
                new Ids.SceneId(string(payload, "scene_id", "set_scene_initial_meta.payload")),
                parseBoundary(object(payload.get("expected_boundary"), "expected_boundary")),
                new SceneSeed(object(payload.get("initial_meta"), "initial_meta"))
        );
    }

    private static EditOperation parseRestore(EditContext context, Map<String, Object> payload) {
        requireKeys(
                payload,
                Set.of("restore_revision_id", "expected_restore_hash"),
                "restore_revision_content.payload"
        );
        return new EditOperation.RestoreRevisionContent(
                context,
                new Ids.RevisionId(string(
                        payload, "restore_revision_id", "restore_revision_content.payload"
                )),
                string(payload, "expected_restore_hash", "restore_revision_content.payload")
        );
    }

    private static BlockDraft parseDraft(Map<String, Object> value) {
        requireKeys(
                value,
                Set.of("id", "text", "meta"),
                Set.of("extensions", "image"),
                "block_draft"
        );
        Map<String, Object> extensions = value.containsKey("extensions")
                ? new LinkedHashMap<>(object(
                        value.get("extensions"), "block_draft.extensions"
                ))
                : new LinkedHashMap<>();
        if (extensions.containsKey(BlockImage.EXTENSION_KEY)) {
            throw new IllegalArgumentException(
                    "block_draft must declare storyblock.image through the image field"
            );
        }
        if (value.containsKey("image")) {
            extensions.put(
                    BlockImage.EXTENSION_KEY,
                    object(value.get("image"), "block_draft.image")
            );
        }
        return new BlockDraft(
                new Ids.BlockId(string(value, "id", "block_draft")),
                string(value, "text", "block_draft"),
                new BlockMetadata(object(value.get("meta"), "block_draft.meta")),
                extensions
        );
    }

    private static BlockRangeGuard parseRange(Map<String, Object> value) {
        requireKeys(value, Set.of(
                "scene_id", "first_block_id", "last_block_id", "expected_blocks",
                "expected_range_hash", "expected_previous_block_id", "expected_next_block_id"
        ), "block_range");
        List<BlockVersionRef> blocks = array(value.get("expected_blocks"), "expected_blocks")
                .stream()
                .map(entry -> parseBlockReference(object(entry, "block_reference")))
                .toList();
        if (!blocks.getFirst().blockId().value().equals(string(value, "first_block_id", "block_range"))
                || !blocks.getLast().blockId().value().equals(
                        string(value, "last_block_id", "block_range")
                )) {
            throw new IllegalArgumentException("Block range endpoints do not match expected_blocks");
        }
        return new BlockRangeGuard(
                new Ids.SceneId(string(value, "scene_id", "block_range")),
                blocks,
                optionalBlockId(value.get("expected_previous_block_id")),
                optionalBlockId(value.get("expected_next_block_id")),
                string(value, "expected_range_hash", "block_range")
        );
    }

    private static BlockVersionRef parseBlockReference(Map<String, Object> value) {
        requireKeys(value, Set.of("block_id", "block_version_id"), "block_reference");
        return new BlockVersionRef(
                new Ids.BlockId(string(value, "block_id", "block_reference")),
                new Ids.BlockVersionId(string(value, "block_version_id", "block_reference"))
        );
    }

    private static InsertionPoint parseInsertionPoint(Map<String, Object> value) {
        requireKeys(
                value,
                Set.of("scene_id", "position"),
                Set.of("anchor_block_id"),
                "insertion_point"
        );
        return new InsertionPoint(
                new Ids.SceneId(string(value, "scene_id", "insertion_point")),
                value.containsKey("anchor_block_id")
                        ? new Ids.BlockId(string(value, "anchor_block_id", "insertion_point"))
                        : null,
                InsertionPoint.Position.valueOf(
                        string(value, "position", "insertion_point")
                                .toUpperCase(java.util.Locale.ROOT)
                )
        );
    }

    private static SceneBoundaryContract parseBoundary(Map<String, Object> value) {
        requireKeys(value, Set.of(
                "scene_id", "first_block_id", "last_block_id", "expected_sequence_hash"
        ), "scene_boundary");
        return new SceneBoundaryContract(
                new Ids.SceneId(string(value, "scene_id", "scene_boundary")),
                optionalBlockId(value.get("first_block_id")),
                optionalBlockId(value.get("last_block_id")),
                string(value, "expected_sequence_hash", "scene_boundary")
        );
    }

    private static Ids.BlockId optionalBlockId(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException("Optional block ID must be a string or null");
        }
        return new Ids.BlockId(string);
    }

    private static int exactInt(Object value, String path) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(path + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(path + " must be an exact 32-bit integer", exception);
        }
    }

    private static String string(Map<String, Object> value, String field, String path) {
        Object entry = value.get(field);
        if (!(entry instanceof String string)) {
            throw new IllegalArgumentException(path + "." + field + " must be a string");
        }
        return string;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }

    private static List<Object> array(Object value, String path) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        return new java.util.ArrayList<>(list);
    }

    private static void requireKeys(
            Map<String, Object> value,
            Set<String> required,
            String path
    ) {
        requireKeys(value, required, Set.of(), path);
    }

    private static void requireKeys(
            Map<String, Object> value,
            Set<String> required,
            Set<String> optional,
            String path
    ) {
        for (String field : required) {
            if (!value.containsKey(field)) {
                throw new IllegalArgumentException(path + " is missing " + field);
            }
        }
        for (String field : value.keySet()) {
            if (!required.contains(field) && !optional.contains(field)) {
                throw new IllegalArgumentException(path + " contains unknown field " + field);
            }
        }
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
        draft.image().ifPresent(image -> result.put("image", image.canonicalValue()));
        Map<String, Object> publicExtensions = new LinkedHashMap<>(draft.extensions());
        publicExtensions.remove(BlockImage.EXTENSION_KEY);
        if (!publicExtensions.isEmpty()) {
            result.put("extensions", Map.copyOf(publicExtensions));
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
