package dev.storyblock.application;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.EditInvariantException;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.EditOperationValidator;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.InsertionPoint;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NarrativeEditor {
    private final RevisionLookup revisionLookup;

    public NarrativeEditor(RevisionLookup revisionLookup) {
        this.revisionLookup = Objects.requireNonNull(revisionLookup, "revisionLookup");
    }

    public RevisionManifest apply(
            RevisionManifest base,
            EditOperation operation,
            Ids.RevisionId newRevisionId,
            Instant createdAt
    ) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(newRevisionId, "newRevisionId");
        Objects.requireNonNull(createdAt, "createdAt");
        String actualHeadHash = NarrativeCanonicalMapper.toCanonical(base).contentHash();
        EditOperationValidator.validate(base, actualHeadHash, operation);

        NarrativeNovel updated = switch (operation) {
            case EditOperation.InsertBlocks insert -> applyInsert(base, insert);
            case EditOperation.ReplaceBlockRange replace -> applyReplacement(
                    base, replace.range(), replace.newBlocks()
            );
            case EditOperation.DeleteBlockRange delete -> applyReplacement(
                    base, delete.range(), List.of()
            );
            case EditOperation.SplitBlock split -> applyReplacement(
                    base, split.block(), split.newBlocks()
            );
            case EditOperation.MergeBlocks merge -> applyReplacement(
                    base, merge.range(), List.of(merge.newBlock())
            );
            case EditOperation.ExtendBlock extend -> applyReplacement(
                    base, extend.block(), List.of(extend.replacement())
            );
            case EditOperation.MoveBlockRange move -> applyMove(base, move);
            case EditOperation.CorrectBlockMeta correction -> applyCorrection(base, correction);
            case EditOperation.SetSceneInitialMeta sceneSeed -> applySceneSeed(base, sceneSeed);
            case EditOperation.RestoreRevisionContent restore -> restoreContent(base, restore);
        };

        return new RevisionManifest(newRevisionId, base.id(), createdAt, updated);
    }

    private static NarrativeNovel applyInsert(
            RevisionManifest base,
            EditOperation.InsertBlocks operation
    ) {
        NarrativeScene scene = base.requireScene(operation.insertionPoint().sceneId());
        int index = EditOperationValidator.insertionIndex(scene, operation.insertionPoint());
        NarrativeScene updated = scene.withBlocks(insertDrafts(scene.blocks(), index, operation.blocks()));
        return replaceScene(base.novel(), updated);
    }

    private static NarrativeNovel applyReplacement(
            RevisionManifest base,
            BlockRangeGuard guard,
            List<BlockDraft> replacements
    ) {
        EditOperationValidator.RangeLocation range = EditOperationValidator.validateRange(base, guard);
        List<NarrativeBlock> retained = new ArrayList<>(range.scene().blocks());
        retained.subList(range.firstIndex(), range.lastIndex() + 1).clear();
        NarrativeScene updated = range.scene().withBlocks(
                insertDrafts(retained, range.firstIndex(), replacements)
        );
        return replaceScene(base.novel(), updated);
    }

    private static NarrativeNovel applyMove(
            RevisionManifest base,
            EditOperation.MoveBlockRange operation
    ) {
        EditOperationValidator.RangeLocation source = EditOperationValidator.validateRange(
                base, operation.range()
        );
        List<NarrativeBlock> moving = source.blocks();

        if (source.scene().id().equals(operation.destination().sceneId())) {
            List<NarrativeBlock> retained = new ArrayList<>(source.scene().blocks());
            retained.subList(source.firstIndex(), source.lastIndex() + 1).clear();
            int destinationIndex = insertionIndexAfterRemoval(retained, operation.destination());
            NarrativeScene updated = source.scene().withBlocks(
                    insertExisting(retained, destinationIndex, moving)
            );
            return replaceScene(base.novel(), updated);
        }

        NarrativeScene destination = base.requireScene(operation.destination().sceneId());
        List<NarrativeBlock> sourceBlocks = new ArrayList<>(source.scene().blocks());
        sourceBlocks.subList(source.firstIndex(), source.lastIndex() + 1).clear();
        int destinationIndex = EditOperationValidator.insertionIndex(
                destination, operation.destination()
        );
        NarrativeScene updatedSource = source.scene().withBlocks(sourceBlocks);
        NarrativeScene updatedDestination = destination.withBlocks(
                insertExisting(destination.blocks(), destinationIndex, moving)
        );
        return replaceScene(replaceScene(base.novel(), updatedSource), updatedDestination);
    }

    private static NarrativeNovel applyCorrection(
            RevisionManifest base,
            EditOperation.CorrectBlockMeta operation
    ) {
        NarrativeScene scene = base.requireScene(operation.sceneId());
        List<NarrativeBlock> blocks = new ArrayList<>(scene.blocks());
        int index = indexOf(blocks, operation.block().blockId());
        NarrativeBlock current = blocks.get(index);
        blocks.set(index, current.revise(current.text(), operation.correctedMetadata(), current.extensions()));
        return replaceScene(base.novel(), scene.withBlocks(blocks));
    }

    private static NarrativeNovel applySceneSeed(
            RevisionManifest base,
            EditOperation.SetSceneInitialMeta operation
    ) {
        NarrativeScene scene = base.requireScene(operation.sceneId());
        return replaceScene(base.novel(), scene.withInitialMeta(operation.initialMeta()));
    }

    private NarrativeNovel restoreContent(
            RevisionManifest base,
            EditOperation.RestoreRevisionContent operation
    ) {
        RevisionManifest target = revisionLookup.require(operation.restoreRevisionId());
        if (!base.novel().id().equals(target.novel().id())) {
            throw invalid("Cannot restore content from another novel");
        }
        String targetHash = NarrativeCanonicalMapper.toCanonical(target).contentHash();
        if (!targetHash.equals(operation.expectedRestoreHash())) {
            throw new EditInvariantException(
                    EditInvariantException.Code.REVISION_CONFLICT,
                    "Restore revision hash does not match the requested target"
            );
        }
        return target.novel();
    }

    private static List<NarrativeBlock> insertDrafts(
            List<NarrativeBlock> retained,
            int insertionIndex,
            List<BlockDraft> drafts
    ) {
        if (drafts.isEmpty()) {
            return List.copyOf(retained);
        }
        try {
            OrderKey left = insertionIndex == 0 ? null : retained.get(insertionIndex - 1).orderKey();
            OrderKey right = insertionIndex == retained.size() ? null : retained.get(insertionIndex).orderKey();
            List<NarrativeBlock> inserted = new ArrayList<>(retained);
            int offset = 0;
            for (BlockDraft draft : drafts) {
                OrderKey key = OrderKey.between(left, right);
                inserted.add(insertionIndex + offset, draft.materialize(key));
                left = key;
                offset++;
            }
            return List.copyOf(inserted);
        } catch (IllegalStateException exhaustedOrderSpace) {
            return rebalanceWithDrafts(retained, insertionIndex, drafts);
        }
    }

    private static List<NarrativeBlock> insertExisting(
            List<NarrativeBlock> retained,
            int insertionIndex,
            List<NarrativeBlock> moving
    ) {
        try {
            OrderKey left = insertionIndex == 0 ? null : retained.get(insertionIndex - 1).orderKey();
            OrderKey right = insertionIndex == retained.size() ? null : retained.get(insertionIndex).orderKey();
            List<NarrativeBlock> inserted = new ArrayList<>(retained);
            int offset = 0;
            for (NarrativeBlock block : moving) {
                OrderKey key = OrderKey.between(left, right);
                inserted.add(insertionIndex + offset, block.moveTo(key));
                left = key;
                offset++;
            }
            return List.copyOf(inserted);
        } catch (IllegalStateException exhaustedOrderSpace) {
            List<NarrativeBlock> sequence = new ArrayList<>(retained);
            sequence.addAll(insertionIndex, moving);
            return rebalance(sequence);
        }
    }

    private static List<NarrativeBlock> rebalanceWithDrafts(
            List<NarrativeBlock> retained,
            int insertionIndex,
            List<BlockDraft> drafts
    ) {
        int total = retained.size() + drafts.size();
        List<NarrativeBlock> result = new ArrayList<>(total);
        int retainedIndex = 0;
        int draftIndex = 0;
        for (int index = 0; index < total; index++) {
            OrderKey key = OrderKey.rebalanced(index, total);
            if (index >= insertionIndex && draftIndex < drafts.size()) {
                result.add(drafts.get(draftIndex++).materialize(key));
            } else {
                result.add(retained.get(retainedIndex++).moveTo(key));
            }
        }
        return List.copyOf(result);
    }

    private static List<NarrativeBlock> rebalance(List<NarrativeBlock> blocks) {
        List<NarrativeBlock> result = new ArrayList<>(blocks.size());
        for (int index = 0; index < blocks.size(); index++) {
            result.add(blocks.get(index).moveTo(OrderKey.rebalanced(index, blocks.size())));
        }
        return List.copyOf(result);
    }

    private static int insertionIndexAfterRemoval(
            List<NarrativeBlock> retained,
            InsertionPoint point
    ) {
        return switch (point.position()) {
            case START -> 0;
            case END -> retained.size();
            case BEFORE -> indexOf(retained, point.anchorBlockId());
            case AFTER -> indexOf(retained, point.anchorBlockId()) + 1;
        };
    }

    private static int indexOf(List<NarrativeBlock> blocks, Ids.BlockId id) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).id().equals(id)) {
                return index;
            }
        }
        throw invalid("Block is not present in the expected scene: " + id.value());
    }

    private static NarrativeNovel replaceScene(NarrativeNovel novel, NarrativeScene replacement) {
        List<NarrativeChapter> chapters = new ArrayList<>(novel.chapters());
        for (int chapterIndex = 0; chapterIndex < chapters.size(); chapterIndex++) {
            NarrativeChapter chapter = chapters.get(chapterIndex);
            List<NarrativeScene> scenes = new ArrayList<>(chapter.scenes());
            for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
                if (scenes.get(sceneIndex).id().equals(replacement.id())) {
                    scenes.set(sceneIndex, replacement);
                    chapters.set(chapterIndex, chapter.withScenes(scenes));
                    return novel.withChapters(chapters);
                }
            }
        }
        throw invalid("Replacement scene is not part of the novel: " + replacement.id().value());
    }

    private static EditInvariantException invalid(String message) {
        return new EditInvariantException(EditInvariantException.Code.INVALID_OPERATION, message);
    }
}
