package dev.storyblock.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.BlockSequenceHash;
import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditInvariantException;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.InsertionPoint;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneBoundaryContract;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NarrativeEditorTest {
    private static final Instant NEXT_REVISION_TIME = Instant.parse("2026-08-21T12:01:00Z");

    @Test
    void insertReplaceAndDeleteAreAtomicImmutableRevisions() {
        RevisionManifest base = fixture();
        NarrativeScene firstScene = firstScene(base);
        NarrativeEditor editor = editorWithoutRestores();

        BlockDraft inserted = BlockDraft.create("新增一句。", BlockMetadata.empty());
        RevisionManifest afterInsert = editor.apply(
                base,
                new EditOperation.InsertBlocks(
                        context(base, "insert"),
                        InsertionPoint.after(firstScene.id(), firstScene.blocks().getFirst().id()),
                        List.of(inserted)
                ),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        assertEquals(4, firstScene(afterInsert).blocks().size());
        assertEquals(3, firstScene(base).blocks().size());
        assertEquals(base.id(), afterInsert.parentId());

        BlockRangeGuard replaceRange = BlockRangeGuard.capture(
                firstScene,
                firstScene.blocks().getFirst().id(),
                firstScene.blocks().get(1).id()
        );
        BlockDraft replacement = BlockDraft.create("合併成一句。", BlockMetadata.empty());
        RevisionManifest afterReplace = editor.apply(
                base,
                new EditOperation.ReplaceBlockRange(
                        context(base, "replace"), replaceRange, List.of(replacement)
                ),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        assertEquals(2, firstScene(afterReplace).blocks().size());
        assertEquals(replacement.id(), firstScene(afterReplace).blocks().getFirst().id());

        BlockRangeGuard deleteRange = BlockRangeGuard.capture(
                firstScene,
                firstScene.blocks().get(1).id(),
                firstScene.blocks().getLast().id()
        );
        RevisionManifest afterDelete = editor.apply(
                base,
                new EditOperation.DeleteBlockRange(context(base, "delete"), deleteRange),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        assertEquals(1, firstScene(afterDelete).blocks().size());
    }

    @Test
    void splitMergeAndExtendEnforceTextSemanticsAndVersioning() {
        RevisionManifest base = fixture();
        NarrativeScene scene = firstScene(base);
        NarrativeEditor editor = editorWithoutRestores();
        NarrativeBlock twoSentences = scene.blocks().get(1);
        int splitAnchor = dev.storyblock.domain.UnicodeText.graphemeCount("第二句。");
        BlockRangeGuard splitRange = BlockRangeGuard.capture(scene, twoSentences.id(), twoSentences.id());
        RevisionManifest split = editor.apply(
                base,
                new EditOperation.SplitBlock(
                        context(base, "split"),
                        splitRange,
                        splitAnchor,
                        List.of(
                                new BlockDraft(
                                        twoSentences.id(), "第二句。", twoSentences.metadata(), Map.of()
                                ),
                                BlockDraft.create("第三句。", BlockMetadata.empty())
                        )
                ),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        assertEquals(4, firstScene(split).blocks().size());
        assertNotEquals(twoSentences.versionId(), firstScene(split).blocks().get(1).versionId());

        BlockRangeGuard mergeRange = BlockRangeGuard.capture(
                scene, scene.blocks().getFirst().id(), scene.blocks().get(1).id()
        );
        RevisionManifest merged = editor.apply(
                base,
                new EditOperation.MergeBlocks(
                        context(base, "merge"),
                        mergeRange,
                        BlockDraft.create("第一句。第二句。", BlockMetadata.empty())
                ),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        assertEquals(2, firstScene(merged).blocks().size());

        NarrativeBlock original = scene.blocks().getFirst();
        BlockRangeGuard extendRange = BlockRangeGuard.capture(scene, original.id(), original.id());
        RevisionManifest extended = editor.apply(
                base,
                new EditOperation.ExtendBlock(
                        context(base, "extend"),
                        extendRange,
                        EditOperation.ExtensionPosition.AFTER,
                        new BlockDraft(
                                original.id(),
                                original.text() + "補充一句。",
                                original.metadata(),
                                original.extensions()
                        )
                ),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        NarrativeBlock extendedBlock = firstScene(extended).blocks().getFirst();
        assertEquals(original.id(), extendedBlock.id());
        assertNotEquals(original.versionId(), extendedBlock.versionId());
    }

    @Test
    void movePreservesStableAndVersionIdentityAcrossScenes() {
        RevisionManifest base = fixture();
        NarrativeScene source = firstScene(base);
        NarrativeScene destination = secondScene(base);
        NarrativeBlock moving = source.blocks().getFirst();
        EditOperation.MoveBlockRange operation = new EditOperation.MoveBlockRange(
                context(base, "move"),
                BlockRangeGuard.capture(source, moving.id(), moving.id()),
                InsertionPoint.endOf(destination.id()),
                SceneBoundaryContract.capture(source),
                SceneBoundaryContract.capture(destination)
        );

        RevisionManifest moved = editorWithoutRestores().apply(
                base, operation, Ids.RevisionId.create(), NEXT_REVISION_TIME
        );
        NarrativeBlock movedBlock = secondScene(moved).blocks().getLast();

        assertEquals(moving.id(), movedBlock.id());
        assertEquals(moving.versionId(), movedBlock.versionId());
        assertEquals(2, firstScene(moved).blocks().size());
        assertEquals(2, secondScene(moved).blocks().size());
    }

    @Test
    void metadataAndSceneSeedCorrectionsAreTypedAndCreateExpectedVersions() {
        RevisionManifest base = fixture();
        NarrativeScene scene = firstScene(base);
        NarrativeBlock block = scene.blocks().getFirst();
        BlockMetadata corrected = new BlockMetadata(Map.of("narrative_mode", "action"));
        RevisionManifest metadataRevision = editorWithoutRestores().apply(
                base,
                new EditOperation.CorrectBlockMeta(
                        context(base, "meta"),
                        scene.id(),
                        BlockVersionRef.from(block),
                        corrected
                ),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        NarrativeBlock correctedBlock = firstScene(metadataRevision).blocks().getFirst();
        assertEquals(block.id(), correctedBlock.id());
        assertNotEquals(block.versionId(), correctedBlock.versionId());
        assertEquals(corrected, correctedBlock.metadata());

        SceneSeed seed = new SceneSeed(Map.of("present_character_ids", List.of("char_new")));
        RevisionManifest seedRevision = editorWithoutRestores().apply(
                base,
                new EditOperation.SetSceneInitialMeta(
                        context(base, "seed"), scene.id(), SceneBoundaryContract.capture(scene), seed
                ),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        assertEquals(seed, firstScene(seedRevision).initialMeta());
        assertSame(block, firstScene(seedRevision).blocks().getFirst());
    }

    @Test
    void restoreCreatesANewRevisionUsingTheExactHistoricalSelection() {
        RevisionManifest historical = fixture();
        NarrativeScene scene = firstScene(historical);
        RevisionManifest current = editorWithoutRestores().apply(
                historical,
                new EditOperation.DeleteBlockRange(
                        context(historical, "prepare-current"),
                        BlockRangeGuard.capture(scene, scene.blocks().getLast().id(), scene.blocks().getLast().id())
                ),
                Ids.RevisionId.create(),
                NEXT_REVISION_TIME
        );
        String historicalHash = NarrativeCanonicalMapper.toCanonical(historical).contentHash();
        NarrativeEditor editor = new NarrativeEditor(id -> {
            if (!id.equals(historical.id())) {
                throw new IllegalArgumentException("Unknown revision");
            }
            return historical;
        });

        RevisionManifest restored = editor.apply(
                current,
                new EditOperation.RestoreRevisionContent(
                        context(current, "restore"), historical.id(), historicalHash
                ),
                Ids.RevisionId.create(),
                Instant.parse("2026-08-21T12:02:00Z")
        );

        assertEquals(current.id(), restored.parentId());
        assertEquals(historical.selectedBlockVersions(), restored.selectedBlockVersions());
        assertNotEquals(historical.id(), restored.id());
    }

    @Test
    void staleVersionNonAdjacentRangeAndHeadHashAreRejected() {
        RevisionManifest base = fixture();
        NarrativeScene scene = firstScene(base);
        BlockRangeGuard valid = BlockRangeGuard.capture(
                scene, scene.blocks().getFirst().id(), scene.blocks().get(1).id()
        );
        BlockRangeGuard staleVersion = new BlockRangeGuard(
                scene.id(),
                List.of(
                        new BlockVersionRef(
                                valid.expectedBlocks().getFirst().blockId(),
                                Ids.BlockVersionId.create()
                        ),
                        valid.expectedBlocks().get(1)
                ),
                valid.expectedPreviousBlockId(),
                valid.expectedNextBlockId(),
                valid.expectedRangeHash()
        );

        EditInvariantException versionFailure = assertThrows(
                EditInvariantException.class,
                () -> editorWithoutRestores().apply(
                        base,
                        new EditOperation.DeleteBlockRange(context(base, "stale-version"), staleVersion),
                        Ids.RevisionId.create(),
                        NEXT_REVISION_TIME
                )
        );
        assertEquals(EditInvariantException.Code.BLOCK_VERSION_CONFLICT, versionFailure.code());

        List<BlockVersionRef> nonAdjacentRefs = List.of(
                BlockVersionRef.from(scene.blocks().getFirst()),
                BlockVersionRef.from(scene.blocks().getLast())
        );
        BlockRangeGuard nonAdjacent = new BlockRangeGuard(
                scene.id(),
                nonAdjacentRefs,
                null,
                null,
                BlockSequenceHash.ofReferences(nonAdjacentRefs)
        );
        EditInvariantException adjacencyFailure = assertThrows(
                EditInvariantException.class,
                () -> editorWithoutRestores().apply(
                        base,
                        new EditOperation.DeleteBlockRange(context(base, "non-adjacent"), nonAdjacent),
                        Ids.RevisionId.create(),
                        NEXT_REVISION_TIME
                )
        );
        assertEquals(EditInvariantException.Code.INVALID_BLOCK_ADJACENCY, adjacencyFailure.code());

        EditContext wrongHead = new EditContext(
                Ids.OperationId.create(),
                "wrong-head",
                base.novel().id(),
                base.id(),
                "sha256:" + "0".repeat(64)
        );
        EditInvariantException headFailure = assertThrows(
                EditInvariantException.class,
                () -> editorWithoutRestores().apply(
                        base,
                        new EditOperation.DeleteBlockRange(wrongHead, valid),
                        Ids.RevisionId.create(),
                        NEXT_REVISION_TIME
                )
        );
        assertEquals(EditInvariantException.Code.REVISION_CONFLICT, headFailure.code());
    }

    private static NarrativeEditor editorWithoutRestores() {
        return new NarrativeEditor(id -> {
            throw new IllegalArgumentException("No restore revision configured: " + id.value());
        });
    }

    private static EditContext context(RevisionManifest base, String idempotencyKey) {
        return new EditContext(
                Ids.OperationId.create(),
                idempotencyKey,
                base.novel().id(),
                base.id(),
                NarrativeCanonicalMapper.toCanonical(base).contentHash()
        );
    }

    private static RevisionManifest fixture() {
        Ids.NovelId novelId = Ids.NovelId.create();
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        NarrativeScene first = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.rebalanced(0, 2),
                "First scene",
                TransitionMode.OPENING,
                SceneSeed.empty(),
                List.of(
                        block(0, 3, "第一句。"),
                        block(1, 3, "第二句。第三句。"),
                        block(2, 3, "第四句。")
                ),
                Map.of()
        );
        NarrativeScene second = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.rebalanced(1, 2),
                "Second scene",
                TransitionMode.CUT,
                SceneSeed.empty(),
                List.of(block(0, 1, "另一幕。")),
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId,
                OrderKey.initial(),
                "Chapter",
                List.of(first, second),
                Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(novelId, List.of(chapter), Map.of())
        );
    }

    private static NarrativeBlock block(int index, int total, String text) {
        return NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(index, total),
                text,
                BlockMetadata.empty(),
                Map.of()
        );
    }

    private static NarrativeScene firstScene(RevisionManifest revision) {
        return revision.novel().chapters().getFirst().scenes().getFirst();
    }

    private static NarrativeScene secondScene(RevisionManifest revision) {
        return revision.novel().chapters().getFirst().scenes().get(1);
    }
}
