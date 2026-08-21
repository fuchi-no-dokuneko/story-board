package dev.storyblock.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public sealed interface EditOperation permits
        EditOperation.InsertBlocks,
        EditOperation.ReplaceBlockRange,
        EditOperation.DeleteBlockRange,
        EditOperation.SplitBlock,
        EditOperation.MergeBlocks,
        EditOperation.ExtendBlock,
        EditOperation.MoveBlockRange,
        EditOperation.CorrectBlockMeta,
        EditOperation.SetSceneInitialMeta,
        EditOperation.RestoreRevisionContent {

    enum Type {
        INSERT_BLOCKS("insert_blocks"),
        REPLACE_BLOCK_RANGE("replace_block_range"),
        DELETE_BLOCK_RANGE("delete_block_range"),
        SPLIT_BLOCK("split_block"),
        MERGE_BLOCKS("merge_blocks"),
        EXTEND_BLOCK("extend_block"),
        MOVE_BLOCK_RANGE("move_block_range"),
        CORRECT_BLOCK_META("correct_block_meta"),
        SET_SCENE_INITIAL_META("set_scene_initial_meta"),
        RESTORE_REVISION_CONTENT("restore_revision_content");

        private final String canonicalName;

        Type(String canonicalName) {
            this.canonicalName = canonicalName;
        }

        public String canonicalName() {
            return canonicalName;
        }
    }

    enum ExtensionPosition {
        BEFORE,
        AFTER
    }

    EditContext context();

    Type type();

    record InsertBlocks(
            EditContext context,
            InsertionPoint insertionPoint,
            List<BlockDraft> blocks
    ) implements EditOperation {
        public InsertBlocks {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(insertionPoint, "insertionPoint");
            blocks = requireDistinctDrafts(blocks, 1, "insert_blocks");
        }

        @Override
        public Type type() {
            return Type.INSERT_BLOCKS;
        }
    }

    record ReplaceBlockRange(
            EditContext context,
            BlockRangeGuard range,
            List<BlockDraft> newBlocks
    ) implements EditOperation {
        public ReplaceBlockRange {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(range, "range");
            newBlocks = requireDistinctDrafts(newBlocks, 1, "replace_block_range");
        }

        @Override
        public Type type() {
            return Type.REPLACE_BLOCK_RANGE;
        }
    }

    record DeleteBlockRange(EditContext context, BlockRangeGuard range) implements EditOperation {
        public DeleteBlockRange {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(range, "range");
        }

        @Override
        public Type type() {
            return Type.DELETE_BLOCK_RANGE;
        }
    }

    record SplitBlock(
            EditContext context,
            BlockRangeGuard block,
            int splitAfterGrapheme,
            List<BlockDraft> newBlocks
    ) implements EditOperation {
        public SplitBlock {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(block, "block");
            if (block.expectedBlocks().size() != 1) {
                throw new IllegalArgumentException("split_block must guard exactly one block");
            }
            if (splitAfterGrapheme < 1) {
                throw new IllegalArgumentException("Split anchor must be a positive grapheme offset");
            }
            newBlocks = requireDistinctDrafts(newBlocks, 2, "split_block");
            if (newBlocks.size() != 2) {
                throw new IllegalArgumentException("split_block must produce exactly two blocks");
            }
        }

        @Override
        public Type type() {
            return Type.SPLIT_BLOCK;
        }

        public BlockProvenanceMapping provenanceMapping() {
            return BlockProvenanceMapping.split(block.expectedBlocks().getFirst(), newBlocks);
        }
    }

    record MergeBlocks(
            EditContext context,
            BlockRangeGuard range,
            BlockDraft newBlock
    ) implements EditOperation {
        public MergeBlocks {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(range, "range");
            Objects.requireNonNull(newBlock, "newBlock");
            if (range.expectedBlocks().size() < 2) {
                throw new IllegalArgumentException("merge_blocks requires at least two blocks");
            }
        }

        @Override
        public Type type() {
            return Type.MERGE_BLOCKS;
        }

        public BlockProvenanceMapping provenanceMapping() {
            return BlockProvenanceMapping.merge(range.expectedBlocks(), newBlock);
        }
    }

    record ExtendBlock(
            EditContext context,
            BlockRangeGuard block,
            ExtensionPosition position,
            BlockDraft replacement
    ) implements EditOperation {
        public ExtendBlock {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(replacement, "replacement");
            if (block.expectedBlocks().size() != 1) {
                throw new IllegalArgumentException("extend_block must guard exactly one block");
            }
        }

        @Override
        public Type type() {
            return Type.EXTEND_BLOCK;
        }
    }

    record MoveBlockRange(
            EditContext context,
            BlockRangeGuard range,
            InsertionPoint destination,
            SceneBoundaryContract expectedSourceBoundary,
            SceneBoundaryContract expectedDestinationBoundary
    ) implements EditOperation {
        public MoveBlockRange {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(range, "range");
            Objects.requireNonNull(destination, "destination");
            Objects.requireNonNull(expectedSourceBoundary, "expectedSourceBoundary");
            Objects.requireNonNull(expectedDestinationBoundary, "expectedDestinationBoundary");
            if (!expectedSourceBoundary.sceneId().equals(range.sceneId())) {
                throw new IllegalArgumentException("Source boundary must describe the guarded scene");
            }
            if (!expectedDestinationBoundary.sceneId().equals(destination.sceneId())) {
                throw new IllegalArgumentException("Destination boundary must describe the destination scene");
            }
        }

        @Override
        public Type type() {
            return Type.MOVE_BLOCK_RANGE;
        }
    }

    record CorrectBlockMeta(
            EditContext context,
            Ids.SceneId sceneId,
            BlockVersionRef block,
            BlockMetadata correctedMetadata
    ) implements EditOperation {
        public CorrectBlockMeta {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(sceneId, "sceneId");
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(correctedMetadata, "correctedMetadata");
        }

        @Override
        public Type type() {
            return Type.CORRECT_BLOCK_META;
        }
    }

    record SetSceneInitialMeta(
            EditContext context,
            Ids.SceneId sceneId,
            SceneBoundaryContract expectedBoundary,
            SceneSeed initialMeta
    ) implements EditOperation {
        public SetSceneInitialMeta {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(sceneId, "sceneId");
            Objects.requireNonNull(expectedBoundary, "expectedBoundary");
            Objects.requireNonNull(initialMeta, "initialMeta");
            if (!sceneId.equals(expectedBoundary.sceneId())) {
                throw new IllegalArgumentException("Scene seed boundary must describe the target scene");
            }
        }

        @Override
        public Type type() {
            return Type.SET_SCENE_INITIAL_META;
        }
    }

    record RestoreRevisionContent(
            EditContext context,
            Ids.RevisionId restoreRevisionId,
            String expectedRestoreHash
    ) implements EditOperation {
        private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

        public RestoreRevisionContent {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(restoreRevisionId, "restoreRevisionId");
            if (expectedRestoreHash == null || !SHA_256.matcher(expectedRestoreHash).matches()) {
                throw new IllegalArgumentException("Expected restore hash must be lowercase SHA-256");
            }
        }

        @Override
        public Type type() {
            return Type.RESTORE_REVISION_CONTENT;
        }
    }

    private static List<BlockDraft> requireDistinctDrafts(
            List<BlockDraft> drafts,
            int minimum,
            String operation
    ) {
        drafts = List.copyOf(drafts);
        if (drafts.size() < minimum) {
            throw new IllegalArgumentException(operation + " requires at least " + minimum + " block(s)");
        }
        Set<Ids.BlockId> ids = new HashSet<>();
        for (BlockDraft draft : drafts) {
            Objects.requireNonNull(draft, "block draft");
            if (!ids.add(draft.id())) {
                throw new IllegalArgumentException(operation + " cannot repeat a draft block ID");
            }
        }
        return drafts;
    }
}
