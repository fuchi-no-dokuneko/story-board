package dev.storyblock.contracts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.InsertionPoint;
import dev.storyblock.domain.SceneBoundaryContract;
import dev.storyblock.domain.SceneSeed;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EditOperationCanonicalMapperTest {
    private static final String HASH_A = "sha256:" + "a".repeat(64);
    private static final String HASH_B = "sha256:" + "b".repeat(64);

    @Test
    void allTenTypedOperationsRoundTripWithoutCanonicalDrift() {
        Ids.NovelId novelId = Ids.NovelId.create();
        Ids.RevisionId baseId = Ids.RevisionId.create();
        Ids.SceneId sourceScene = Ids.SceneId.create();
        Ids.SceneId destinationScene = Ids.SceneId.create();
        Ids.BlockId firstId = Ids.BlockId.create();
        Ids.BlockId secondId = Ids.BlockId.create();
        BlockVersionRef first = new BlockVersionRef(
                firstId, Ids.BlockVersionId.create()
        );
        BlockVersionRef second = new BlockVersionRef(
                secondId, Ids.BlockVersionId.create()
        );
        BlockRangeGuard one = new BlockRangeGuard(
                sourceScene, List.of(first), null, secondId, HASH_A
        );
        BlockRangeGuard two = new BlockRangeGuard(
                sourceScene, List.of(first, second), null, null, HASH_B
        );
        BlockDraft firstDraft = new BlockDraft(
                firstId,
                "第一句。",
                new BlockMetadata(Map.of("narrative_mode", "action")),
                Map.of("example.test", Map.of("rank", 1))
        );
        BlockDraft secondDraft = BlockDraft.create("第二句。", BlockMetadata.empty());
        SceneBoundaryContract sourceBoundary = new SceneBoundaryContract(
                sourceScene, firstId, secondId, HASH_A
        );
        SceneBoundaryContract destinationBoundary = new SceneBoundaryContract(
                destinationScene, null, null, HASH_B
        );

        List<EditOperation> operations = List.of(
                new EditOperation.InsertBlocks(
                        context(novelId, baseId, "insert"),
                        InsertionPoint.after(sourceScene, firstId),
                        List.of(secondDraft)
                ),
                new EditOperation.ReplaceBlockRange(
                        context(novelId, baseId, "replace"), one, List.of(firstDraft)
                ),
                new EditOperation.DeleteBlockRange(
                        context(novelId, baseId, "delete"), one
                ),
                new EditOperation.SplitBlock(
                        context(novelId, baseId, "split"),
                        one,
                        3,
                        List.of(firstDraft, secondDraft)
                ),
                new EditOperation.MergeBlocks(
                        context(novelId, baseId, "merge"), two, firstDraft
                ),
                new EditOperation.ExtendBlock(
                        context(novelId, baseId, "extend"),
                        one,
                        EditOperation.ExtensionPosition.AFTER,
                        firstDraft
                ),
                new EditOperation.MoveBlockRange(
                        context(novelId, baseId, "move"),
                        two,
                        InsertionPoint.endOf(destinationScene),
                        sourceBoundary,
                        destinationBoundary
                ),
                new EditOperation.CorrectBlockMeta(
                        context(novelId, baseId, "meta"),
                        sourceScene,
                        first,
                        new BlockMetadata(Map.of("narrative_mode", "dialogue"))
                ),
                new EditOperation.SetSceneInitialMeta(
                        context(novelId, baseId, "scene-meta"),
                        sourceScene,
                        sourceBoundary,
                        new SceneSeed(Map.of("present_character_ids", List.of("char_a")))
                ),
                new EditOperation.RestoreRevisionContent(
                        context(novelId, baseId, "restore"),
                        Ids.RevisionId.create(),
                        HASH_B
                )
        );

        for (EditOperation operation : operations) {
            Map<String, Object> canonical = EditOperationCanonicalMapper.toCanonical(operation);
            EditOperation decoded = EditOperationCanonicalMapper.fromCanonical(
                    CanonicalJson.bytes(canonical)
            );

            assertArrayEquals(
                    CanonicalJson.bytes(canonical),
                    CanonicalJson.bytes(EditOperationCanonicalMapper.toCanonical(decoded))
            );
            assertEquals(
                    EditOperationCanonicalMapper.hash(operation),
                    EditOperationCanonicalMapper.hash(decoded)
            );
        }
    }

    @Test
    void unknownEnvelopeFieldsAreRejected() {
        Ids.NovelId novelId = Ids.NovelId.create();
        Ids.RevisionId baseId = Ids.RevisionId.create();
        EditOperation operation = new EditOperation.RestoreRevisionContent(
                context(novelId, baseId, "strict"), Ids.RevisionId.create(), HASH_B
        );
        Map<String, Object> tampered = new LinkedHashMap<>(
                EditOperationCanonicalMapper.toCanonical(operation)
        );
        tampered.put("unexpected", true);

        assertThrows(
                IllegalArgumentException.class,
                () -> EditOperationCanonicalMapper.fromCanonical(tampered)
        );
    }

    @Test
    void imageDraftUsesExplicitWireFieldAndCanonicalExtensionStorage() {
        BlockImage image = new BlockImage(
                Ids.ArtifactId.create(),
                HASH_A,
                "image/png",
                640,
                480,
                "白色背景上的角色參考圖。"
        );
        BlockDraft draft = new BlockDraft(
                Ids.BlockId.create(),
                "角色參考圖置於此處。",
                BlockMetadata.empty(),
                image.attachTo(Map.of("example.test", true))
        );
        EditOperation operation = new EditOperation.InsertBlocks(
                context(Ids.NovelId.create(), Ids.RevisionId.create(), "image"),
                InsertionPoint.endOf(Ids.SceneId.create()),
                List.of(draft)
        );

        Map<String, Object> canonical = EditOperationCanonicalMapper.toCanonical(operation);
        EditOperation decoded = EditOperationCanonicalMapper.fromCanonical(canonical);
        BlockDraft decodedDraft = ((EditOperation.InsertBlocks) decoded).blocks().getFirst();

        assertEquals(image, decodedDraft.image().orElseThrow());
        assertEquals(draft, decodedDraft);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) canonical.get("payload");
        @SuppressWarnings("unchecked")
        Map<String, Object> wireDraft = (Map<String, Object>) ((List<?>) payload.get("blocks"))
                .getFirst();
        assertEquals(image.canonicalValue(), wireDraft.get("image"));
        @SuppressWarnings("unchecked")
        Map<String, Object> extensions = (Map<String, Object>) wireDraft.get("extensions");
        assertEquals(Map.of("example.test", true), extensions);

        Map<String, Object> hiddenImage = new LinkedHashMap<>(canonical);
        Map<String, Object> hiddenPayload = new LinkedHashMap<>(payload);
        Map<String, Object> hiddenDraft = new LinkedHashMap<>(wireDraft);
        hiddenDraft.remove("image");
        hiddenDraft.put("extensions", Map.of(BlockImage.EXTENSION_KEY, image.canonicalValue()));
        hiddenPayload.put("blocks", List.of(hiddenDraft));
        hiddenImage.put("payload", hiddenPayload);
        assertThrows(
                IllegalArgumentException.class,
                () -> EditOperationCanonicalMapper.fromCanonical(hiddenImage)
        );
    }

    private static EditContext context(
            Ids.NovelId novelId,
            Ids.RevisionId baseId,
            String key
    ) {
        return new EditContext(
                Ids.OperationId.create(), key, novelId, baseId, HASH_A
        );
    }
}
