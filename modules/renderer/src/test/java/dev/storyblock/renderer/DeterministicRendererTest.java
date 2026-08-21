package dev.storyblock.renderer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.storyblock.contracts.CanonicalJson;
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

class DeterministicRendererTest {
    private static final String HASH = "sha256:" + "a".repeat(64);

    @Test
    void identicalInputProducesIdenticalBytesAndResolvedState() {
        RevisionManifest revision = revision();
        DeterministicRenderer renderer = new DeterministicRenderer();

        RenderPacket first = renderer.render(revision, HASH, RenderRange.all());
        RenderPacket second = renderer.render(revision, HASH, RenderRange.all());

        assertEquals(first, second);
        assertArrayEquals(CanonicalJson.bytes(first), CanonicalJson.bytes(second));
        assertEquals("乙進入房間。\n他坐下。", first.renderedText());
        assertEquals(List.of("char_a"),
                first.resolvedMetadata().getFirst().before().get("present_character_ids"));
        assertEquals(List.of("char_a", "char_b"),
                first.resolvedMetadata().getFirst().after().get("present_character_ids"));
        assertEquals("rain", first.resolvedMetadata().getFirst().after().get("weather"));
        assertEquals(
                Map.of("mode", "unknown"),
                first.resolvedMetadata().getLast().after().get("weather")
        );
        assertEquals(
                first.blocks().getFirst().text().codePointCount(0, first.blocks().getFirst().text().length()) + 1,
                first.offsetMap().getLast().renderedStart()
        );
    }

    @Test
    void subrangeRetainsStateResolvedFromPrecedingBlocks() {
        RevisionManifest revision = revision();
        Ids.BlockId secondBlock = revision.liveBlocks().getLast().id();

        RenderPacket packet = new DeterministicRenderer().render(
                revision, HASH, RenderRange.inclusive(secondBlock, secondBlock)
        );

        assertEquals(1, packet.blocks().size());
        assertEquals(List.of("char_a", "char_b"),
                packet.resolvedMetadata().getFirst().before().get("present_character_ids"));
        assertEquals("他坐下。", packet.renderedText());
    }

    @Test
    void emptyRevisionProducesAnEmptyAllRangePacket() {
        RevisionManifest populated = revision();
        NarrativeScene original = populated.novel().chapters().getFirst().scenes().getFirst();
        NarrativeScene emptyScene = original.withBlocks(List.of());
        NarrativeChapter emptyChapter = new NarrativeChapter(
                populated.novel().chapters().getFirst().id(),
                populated.novel().chapters().getFirst().orderKey(),
                populated.novel().chapters().getFirst().title(),
                List.of(emptyScene),
                populated.novel().chapters().getFirst().extensions()
        );
        RevisionManifest empty = new RevisionManifest(
                Ids.RevisionId.create(),
                populated.id(),
                Instant.parse("2026-08-21T12:01:00Z"),
                new NarrativeNovel(
                        populated.novel().id(), List.of(emptyChapter), populated.novel().extensions()
                )
        );

        RenderPacket packet = new DeterministicRenderer().render(
                empty, HASH, RenderRange.all()
        );

        assertEquals("", packet.renderedText());
        assertEquals(RenderRange.all(), packet.range());
        assertEquals(List.of(), packet.blocks());
        assertEquals(List.of(), packet.resolvedMetadata());
        assertEquals(List.of(), packet.offsetMap());
    }

    private static RevisionManifest revision() {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        NarrativeBlock first = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(0, 2),
                "乙進入房間。",
                new BlockMetadata(Map.of(
                        "weather", Map.of("mode", "inherited"),
                        "presence_events", List.of(Map.of(
                                "type", "enter", "character_id", "char_b"
                        ))
                )),
                Map.of()
        );
        NarrativeBlock second = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(1, 2),
                "他坐下。",
                new BlockMetadata(Map.of("weather", Map.of("mode", "unknown"))),
                Map.of()
        );
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "Room",
                TransitionMode.OPENING,
                new SceneSeed(Map.of(
                        "weather", Map.of("mode", "explicit", "value", "rain"),
                        "present_character_ids", List.of("char_a")
                )),
                List.of(first, second),
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId, OrderKey.initial(), "Chapter", List.of(scene), Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(), List.of(chapter), Map.of())
        );
    }
}
