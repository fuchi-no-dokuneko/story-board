package dev.storyblock.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EditOperationValidator {
    private EditOperationValidator() {
    }

    public static void validate(
            RevisionManifest base,
            String actualHeadHash,
            EditOperation operation
    ) {
        validateContext(base, actualHeadHash, operation.context());
        switch (operation) {
            case EditOperation.InsertBlocks insert -> validateInsert(base, insert);
            case EditOperation.ReplaceBlockRange replace -> {
                validateRange(base, replace.range());
                validateDraftIdentities(base, replace.newBlocks(), rangeIds(replace.range()));
            }
            case EditOperation.DeleteBlockRange delete -> validateRange(base, delete.range());
            case EditOperation.SplitBlock split -> validateSplit(base, split);
            case EditOperation.MergeBlocks merge -> {
                validateRange(base, merge.range());
                validateDraftIdentities(base, List.of(merge.newBlock()), rangeIds(merge.range()));
            }
            case EditOperation.ExtendBlock extend -> validateExtend(base, extend);
            case EditOperation.MoveBlockRange move -> validateMove(base, move);
            case EditOperation.CorrectBlockMeta correction -> validateCorrection(base, correction);
            case EditOperation.SetSceneInitialMeta sceneSeed -> validateSceneSeed(base, sceneSeed);
            case EditOperation.RestoreRevisionContent ignored -> {
                // The target revision and its hash are checked by the application revision lookup.
            }
        }
    }

    public static RangeLocation validateRange(RevisionManifest base, BlockRangeGuard guard) {
        NarrativeScene scene = base.requireScene(guard.sceneId());
        List<NarrativeBlock> blocks = scene.blocks();
        int first = indexOf(blocks, guard.firstBlockId());
        int last = first + guard.expectedBlocks().size() - 1;
        if (last >= blocks.size() || !blocks.get(last).id().equals(guard.lastBlockId())) {
            throw adjacency("Guarded range is not contiguous in the base revision");
        }

        for (int offset = 0; offset < guard.expectedBlocks().size(); offset++) {
            NarrativeBlock actual = blocks.get(first + offset);
            BlockVersionRef expected = guard.expectedBlocks().get(offset);
            if (!actual.id().equals(expected.blockId())) {
                throw adjacency("Guarded range block order no longer matches the base revision");
            }
            if (!actual.versionId().equals(expected.blockVersionId())) {
                throw new EditInvariantException(
                        EditInvariantException.Code.BLOCK_VERSION_CONFLICT,
                        "Block version changed for " + actual.id().value()
                );
            }
        }

        Ids.BlockId previous = first == 0 ? null : blocks.get(first - 1).id();
        Ids.BlockId next = last == blocks.size() - 1 ? null : blocks.get(last + 1).id();
        if (!java.util.Objects.equals(previous, guard.expectedPreviousBlockId())
                || !java.util.Objects.equals(next, guard.expectedNextBlockId())) {
            throw adjacency("Guarded range anchors no longer match the base revision");
        }

        List<NarrativeBlock> actualRange = blocks.subList(first, last + 1);
        if (!BlockSequenceHash.ofBlocks(actualRange).equals(guard.expectedRangeHash())) {
            throw adjacency("Guarded range hash no longer matches the base revision");
        }
        return new RangeLocation(scene, first, last);
    }

    public static int insertionIndex(NarrativeScene scene, InsertionPoint insertionPoint) {
        if (!scene.id().equals(insertionPoint.sceneId())) {
            throw invalid("Insertion point belongs to a different scene");
        }
        return switch (insertionPoint.position()) {
            case START -> 0;
            case END -> scene.blocks().size();
            case BEFORE -> indexOf(scene.blocks(), insertionPoint.anchorBlockId());
            case AFTER -> indexOf(scene.blocks(), insertionPoint.anchorBlockId()) + 1;
        };
    }

    public static void validateBoundary(NarrativeScene scene, SceneBoundaryContract boundary) {
        if (!scene.id().equals(boundary.sceneId())) {
            throw adjacency("Scene boundary identifies a different scene");
        }
        Ids.BlockId first = scene.blocks().isEmpty() ? null : scene.blocks().getFirst().id();
        Ids.BlockId last = scene.blocks().isEmpty() ? null : scene.blocks().getLast().id();
        if (!java.util.Objects.equals(first, boundary.firstBlockId())
                || !java.util.Objects.equals(last, boundary.lastBlockId())
                || !BlockSequenceHash.ofBlocks(scene.blocks()).equals(boundary.expectedSequenceHash())) {
            throw adjacency("Scene boundary contract is stale");
        }
    }

    private static void validateContext(
            RevisionManifest base,
            String actualHeadHash,
            EditContext context
    ) {
        if (!base.novel().id().equals(context.novelId())) {
            throw conflict("Operation novel does not match the loaded revision");
        }
        if (!base.id().equals(context.baseRevisionId())) {
            throw conflict("Operation base revision does not match the loaded revision");
        }
        if (!context.expectedHeadHash().equals(actualHeadHash)) {
            throw conflict("Expected head hash does not match the loaded revision");
        }
    }

    private static void validateInsert(RevisionManifest base, EditOperation.InsertBlocks insert) {
        NarrativeScene scene = base.requireScene(insert.insertionPoint().sceneId());
        insertionIndex(scene, insert.insertionPoint());
        validateDraftIdentities(base, insert.blocks(), Set.of());
    }

    private static void validateSplit(RevisionManifest base, EditOperation.SplitBlock split) {
        RangeLocation range = validateRange(base, split.block());
        NarrativeBlock original = range.blocks().getFirst();
        if (!UnicodeText.analyze(original.text()).safeSplitAnchors().contains(split.splitAfterGrapheme())) {
            throw invalid("split_block anchor is not a sentence, dialogue, or author-approved boundary");
        }
        String joined = split.newBlocks().get(0).text() + split.newBlocks().get(1).text();
        if (!joined.equals(original.text())) {
            throw invalid("split_block output must preserve the original text exactly");
        }
        if (UnicodeText.graphemeCount(split.newBlocks().getFirst().text()) != split.splitAfterGrapheme()) {
            throw invalid("split_block payload does not match its grapheme anchor");
        }
        validateDraftIdentities(base, split.newBlocks(), rangeIds(split.block()));
    }

    private static void validateExtend(RevisionManifest base, EditOperation.ExtendBlock extend) {
        RangeLocation range = validateRange(base, extend.block());
        NarrativeBlock original = range.blocks().getFirst();
        BlockDraft replacement = extend.replacement();
        if (!replacement.id().equals(original.id())) {
            throw invalid("extend_block must preserve the stable block ID");
        }
        boolean extendsCorrectSide = switch (extend.position()) {
            case BEFORE -> replacement.text().endsWith(original.text());
            case AFTER -> replacement.text().startsWith(original.text());
        };
        if (!extendsCorrectSide || replacement.text().equals(original.text())) {
            throw invalid("extend_block replacement must add text on the declared side");
        }
        validateDraftIdentities(base, List.of(replacement), rangeIds(extend.block()));
    }

    private static void validateMove(RevisionManifest base, EditOperation.MoveBlockRange move) {
        RangeLocation source = validateRange(base, move.range());
        NarrativeScene destination = base.requireScene(move.destination().sceneId());
        validateBoundary(source.scene(), move.expectedSourceBoundary());
        validateBoundary(destination, move.expectedDestinationBoundary());
        insertionIndex(destination, move.destination());

        if (source.scene().id().equals(destination.id())) {
            Set<Ids.BlockId> movingIds = rangeIds(move.range());
            if (movingIds.contains(move.destination().anchorBlockId())) {
                throw invalid("A moved range cannot use one of its own blocks as destination anchor");
            }
            int insertion = insertionIndex(destination, move.destination());
            if (insertion == source.firstIndex() || insertion == source.lastIndex() + 1) {
                throw invalid("move_block_range cannot be a no-op");
            }
        }
    }

    private static void validateCorrection(
            RevisionManifest base,
            EditOperation.CorrectBlockMeta correction
    ) {
        NarrativeScene scene = base.requireScene(correction.sceneId());
        int index = indexOf(scene.blocks(), correction.block().blockId());
        NarrativeBlock block = scene.blocks().get(index);
        if (!block.versionId().equals(correction.block().blockVersionId())) {
            throw new EditInvariantException(
                    EditInvariantException.Code.BLOCK_VERSION_CONFLICT,
                    "Block version changed for " + block.id().value()
            );
        }
        if (block.metadata().equals(correction.correctedMetadata())) {
            throw invalid("correct_block_meta cannot submit unchanged metadata");
        }
    }

    private static void validateSceneSeed(
            RevisionManifest base,
            EditOperation.SetSceneInitialMeta operation
    ) {
        NarrativeScene scene = base.requireScene(operation.sceneId());
        validateBoundary(scene, operation.expectedBoundary());
        if (operation.initialMeta().equals(scene.initialMeta())) {
            throw invalid("set_scene_initial_meta cannot submit an unchanged scene seed");
        }
    }

    private static void validateDraftIdentities(
            RevisionManifest base,
            List<BlockDraft> drafts,
            Set<Ids.BlockId> replaceableIds
    ) {
        Set<Ids.BlockId> liveIds = base.selectedBlockVersions().keySet();
        for (BlockDraft draft : drafts) {
            if (liveIds.contains(draft.id()) && !replaceableIds.contains(draft.id())) {
                throw new EditInvariantException(
                        EditInvariantException.Code.DUPLICATE_BLOCK_ID,
                        "Draft reuses a live block ID outside the replaced range: " + draft.id().value()
                );
            }
        }
    }

    private static Set<Ids.BlockId> rangeIds(BlockRangeGuard range) {
        Set<Ids.BlockId> ids = new HashSet<>();
        for (BlockVersionRef block : range.expectedBlocks()) {
            ids.add(block.blockId());
        }
        return Set.copyOf(ids);
    }

    private static int indexOf(List<NarrativeBlock> blocks, Ids.BlockId blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).id().equals(blockId)) {
                return index;
            }
        }
        throw adjacency("Block is not present in the expected scene: " + blockId.value());
    }

    private static EditInvariantException adjacency(String message) {
        return new EditInvariantException(EditInvariantException.Code.INVALID_BLOCK_ADJACENCY, message);
    }

    private static EditInvariantException conflict(String message) {
        return new EditInvariantException(EditInvariantException.Code.REVISION_CONFLICT, message);
    }

    private static EditInvariantException invalid(String message) {
        return new EditInvariantException(EditInvariantException.Code.INVALID_OPERATION, message);
    }

    public record RangeLocation(NarrativeScene scene, int firstIndex, int lastIndex) {
        public List<NarrativeBlock> blocks() {
            return List.copyOf(scene.blocks().subList(firstIndex, lastIndex + 1));
        }
    }
}
