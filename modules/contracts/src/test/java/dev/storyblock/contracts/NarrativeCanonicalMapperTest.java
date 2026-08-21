package dev.storyblock.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NarrativeCanonicalMapperTest {
    @Test
    void hierarchyRoundTripsThroughTheCanonicalContract() {
        RevisionManifest manifest = manifest();

        CanonicalRevision canonical = NarrativeCanonicalMapper.toCanonical(manifest);
        RevisionManifest restored = NarrativeCanonicalMapper.fromCanonical(
                CanonicalRevision.parseEnvelope(canonical.envelopeBytes())
        );

        assertEquals(manifest, restored);
        assertEquals(2, restored.selectedBlockVersions().size());
        assertEquals(
                restored.liveBlocks().getFirst().versionId(),
                restored.selectedBlockVersions().get(restored.liveBlocks().getFirst().id())
        );
    }

    @Test
    void revisionRejectsSelectingOneLiveBlockIdentityTwice() {
        RevisionManifest valid = manifest();
        NarrativeChapter chapter = valid.novel().chapters().getFirst();
        NarrativeScene firstScene = chapter.scenes().getFirst();
        NarrativeScene duplicateScene = new NarrativeScene(
                Ids.SceneId.create(),
                chapter.id(),
                OrderKey.between(firstScene.orderKey(), null),
                "Duplicate",
                TransitionMode.CUT,
                SceneSeed.empty(),
                List.of(firstScene.blocks().getFirst()),
                Map.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new RevisionManifest(
                        valid.id(),
                        valid.parentId(),
                        valid.createdAt(),
                        new NarrativeNovel(
                                valid.novel().id(),
                                List.of(chapter.withScenes(List.of(firstScene, duplicateScene))),
                                Map.of()
                        )
                )
        );
    }

    private static RevisionManifest manifest() {
        Ids.NovelId novelId = Ids.NovelId.create();
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        NarrativeBlock first = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(0, 2),
                "第一句。",
                new BlockMetadata(Map.of("narrative_mode", "narration")),
                Map.of("example.test", Map.of("source", "fixture"))
        );
        NarrativeBlock second = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(1, 2),
                "第二句。",
                BlockMetadata.empty(),
                Map.of()
        );
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "Opening",
                TransitionMode.OPENING,
                new SceneSeed(Map.of("present_character_ids", List.of("char_one"))),
                List.of(first, second),
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId,
                OrderKey.initial(),
                "Chapter 1",
                List.of(scene),
                Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(novelId, List.of(chapter), Map.of())
        );
    }
}
