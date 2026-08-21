package dev.storyblock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EditOperationContractTest {
    @Test
    void exposesExactlyTheTenNamedTypedOperations() {
        Set<String> types = Arrays.stream(EditOperation.Type.values())
                .map(EditOperation.Type::canonicalName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "insert_blocks",
                "replace_block_range",
                "delete_block_range",
                "split_block",
                "merge_blocks",
                "extend_block",
                "move_block_range",
                "correct_block_meta",
                "set_scene_initial_meta",
                "restore_revision_content"
        ), types);
        assertEquals(10, EditOperation.class.getPermittedSubclasses().length);
    }

    @Test
    void localObservationOnlyAllowsValuesForExplicitState() {
        assertEquals(
                java.util.Map.of("mode", "unknown"),
                LocalObservation.unknown().canonicalValue()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new LocalObservation(MetadataValueState.UNKNOWN, "invented")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new LocalObservation(MetadataValueState.EXPLICIT, null)
        );
    }

    @Test
    void splitAndMergeExposeCompleteProvenanceMappings() {
        NarrativeBlock first = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(0, 2),
                "第一句。第二句。",
                BlockMetadata.empty(),
                Map.of()
        );
        NarrativeBlock second = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(1, 2),
                "第三句。",
                BlockMetadata.empty(),
                Map.of()
        );
        Ids.SceneId sceneId = Ids.SceneId.create();
        BlockRangeGuard splitRange = new BlockRangeGuard(
                sceneId,
                List.of(BlockVersionRef.from(first)),
                null,
                second.id(),
                BlockSequenceHash.ofBlocks(List.of(first))
        );
        List<BlockDraft> splitResults = List.of(
                new BlockDraft(first.id(), "第一句。", BlockMetadata.empty(), Map.of()),
                BlockDraft.create("第二句。", BlockMetadata.empty())
        );
        EditOperation.SplitBlock split = new EditOperation.SplitBlock(
                context(), splitRange, UnicodeText.graphemeCount("第一句。"), splitResults
        );
        assertEquals(
                splitResults.stream().map(BlockDraft::id).toList(),
                split.provenanceMapping().sourceToResults().get(BlockVersionRef.from(first))
        );

        List<BlockVersionRef> mergeSources = List.of(
                BlockVersionRef.from(first), BlockVersionRef.from(second)
        );
        BlockRangeGuard mergeRange = new BlockRangeGuard(
                sceneId,
                mergeSources,
                null,
                null,
                BlockSequenceHash.ofReferences(mergeSources)
        );
        BlockDraft mergeResult = BlockDraft.create("合併內容。", BlockMetadata.empty());
        EditOperation.MergeBlocks merge = new EditOperation.MergeBlocks(
                context(), mergeRange, mergeResult
        );
        assertEquals(
                Set.of(BlockVersionRef.from(first), BlockVersionRef.from(second)),
                merge.provenanceMapping().sourceToResults().keySet()
        );
        assertEquals(
                Set.of(List.of(mergeResult.id())),
                Set.copyOf(merge.provenanceMapping().sourceToResults().values())
        );
    }

    private static EditContext context() {
        return new EditContext(
                Ids.OperationId.create(),
                "test",
                Ids.NovelId.create(),
                Ids.RevisionId.create(),
                "sha256:" + "a".repeat(64)
        );
    }
}
