package dev.storyblock.renderer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeterministicRendererTest {
    private static final String HASH = "sha256:" + "a".repeat(64);
    private static final String MIN_ENTERS_HASH =
            "sha256:adcdbf673750ae33de22f40f290f55d516d62e5fb7bfd51b0174e1c49375fab8";
    private static final Ids.NovelId GOLDEN_NOVEL = new Ids.NovelId(id("nov", 1));
    private static final Ids.ChapterId GOLDEN_CHAPTER = new Ids.ChapterId(id("ch", 2));
    private static final Ids.SceneId GOLDEN_SCENE_ONE = new Ids.SceneId(id("scn", 3));
    private static final Ids.SceneId GOLDEN_SCENE_TWO = new Ids.SceneId(id("scn", 4));
    private static final Ids.BlockId GOLDEN_BLOCK_ONE = new Ids.BlockId(id("blk", 5));
    private static final Ids.BlockId GOLDEN_BLOCK_TWO = new Ids.BlockId(id("blk", 6));
    private static final Ids.BlockId GOLDEN_BLOCK_THREE = new Ids.BlockId(id("blk", 7));
    private static final Ids.BlockVersionId GOLDEN_VERSION_ONE =
            new Ids.BlockVersionId(id("blv", 8));
    private static final Ids.BlockVersionId GOLDEN_VERSION_TWO =
            new Ids.BlockVersionId(id("blv", 9));
    private static final Ids.BlockVersionId GOLDEN_VERSION_THREE =
            new Ids.BlockVersionId(id("blv", 10));
    private static final Ids.RevisionId GOLDEN_REVISION = new Ids.RevisionId(id("rev", 11));

    @Test
    void identicalInputProducesIdenticalBytesAndResolvedState() {
        RevisionManifest revision = revision();
        DeterministicRenderer renderer = new DeterministicRenderer();

        RenderPacket first = renderer.render(revision, HASH, RenderRange.all());
        RenderPacket second = renderer.render(revision, HASH, RenderRange.all());

        assertEquals(first, second);
        assertArrayEquals(CanonicalJson.bytes(first), CanonicalJson.bytes(second));
        assertEquals("乙🙂進入房間。\n他坐下。", first.renderedText());
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
        assertArrayEquals(
                CanonicalJson.bytes(first.canonicalValue()),
                CanonicalJson.bytes(second.canonicalValue())
        );
    }

    @Test
    void goldenScenesProduceDocumentedStateTransitions() throws IOException {
        RenderPacket packet = new DeterministicRenderer().render(
                goldenRevision(), HASH, RenderRange.all()
        );
        Map<String, Object> actual = Map.of(
                "resolved_meta", packet.canonicalValue().get("resolved_meta"),
                "scene_boundaries", packet.canonicalValue().get("scene_boundaries")
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> expected = CanonicalJson.parse(
                readFixture("renderer-state-machine.json"), Map.class
        );

        assertArrayEquals(CanonicalJson.bytes(expected), CanonicalJson.bytes(actual));
        assertEquals(2, packet.sceneBoundaries().size());
        assertEquals(
                packet.resolvedMetadata().get(1).after(),
                packet.sceneBoundaries().getFirst().stateOut()
        );
        assertEquals(
                packet.resolvedMetadata().getLast().after(),
                packet.sceneBoundaries().getLast().stateOut()
        );
    }

    @Test
    void everyOffsetReconstructsItsUnicodeBlockSpan() {
        RenderPacket packet = new DeterministicRenderer().render(
                revision(), HASH, RenderRange.all()
        );

        for (int index = 0; index < packet.blocks().size(); index++) {
            RenderedBlock block = packet.blocks().get(index);
            OffsetMapEntry offset = packet.offsetMap().get(index);
            int utf16Start = packet.renderedText().offsetByCodePoints(0, offset.renderedStart());
            int utf16End = packet.renderedText().offsetByCodePoints(0, offset.renderedEnd());

            assertEquals(block.blockId(), offset.blockId());
            assertEquals(block.text(), packet.renderedText().substring(utf16Start, utf16End));
        }
    }

    @Test
    void unknownAndNotApplicableValuesAreNeverPromotedByInheritance() {
        RenderPacket packet = new DeterministicRenderer().render(
                goldenRevision(), HASH, RenderRange.all()
        );

        assertEquals(
                Map.of("mode", "unknown"),
                packet.resolvedMetadata().getFirst().after().get("weather")
        );
        assertEquals(
                Map.of("mode", "unknown"),
                packet.sceneBoundaries().getLast().stateOut().get("time")
        );
        assertEquals(
                Map.of("mode", "not_applicable"),
                packet.sceneBoundaries().getLast().stateOut().get("weather")
        );
    }

    @Test
    void canonicalProjectionUsesStableWireNamesAndScalarIds() {
        RenderPacket packet = new DeterministicRenderer().render(
                goldenRevision(), HASH, RenderRange.all()
        );
        Map<String, Object> canonical = packet.canonicalValue();

        assertEquals(GOLDEN_NOVEL.value(), canonical.get("novel_id"));
        assertEquals("Kai waits.\nMin enters.\nRain stops.", canonical.get("rendered_text"));
        assertTrue(canonical.containsKey("resolved_meta"));
        assertTrue(canonical.containsKey("scene_boundaries"));
        assertTrue(canonical.containsKey("offset_map"));
        assertEquals(CanonicalJson.hash(canonical), CanonicalJson.hash(packet.canonicalValue()));
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
        assertEquals(1, packet.sceneBoundaries().size());
        assertEquals(
                packet.sceneBoundaries().getFirst().stateIn(),
                packet.sceneBoundaries().getFirst().stateOut()
        );
    }

    @Test
    void malformedUntypedObservationCannotBecomeResolvedFact() {
        RevisionManifest source = revision();
        NarrativeScene scene = source.novel().chapters().getFirst().scenes().getFirst();
        NarrativeBlock original = scene.blocks().getFirst();
        NarrativeBlock malformed = new NarrativeBlock(
                original.id(),
                Ids.BlockVersionId.create(),
                original.orderKey(),
                original.text(),
                new BlockMetadata(Map.of("weather", "invented-rain")),
                original.extensions()
        );
        NarrativeScene changedScene = scene.withBlocks(List.of(malformed, scene.blocks().getLast()));
        RevisionManifest changed = withScene(source, changedScene);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new DeterministicRenderer().render(changed, HASH, RenderRange.all())
        );
        assertTrue(failure.getMessage().contains("must declare a metadata mode"));
    }

    private static RevisionManifest revision() {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        NarrativeBlock first = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(0, 2),
                "乙🙂進入房間。",
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

    private static RevisionManifest goldenRevision() {
        NarrativeBlock first = new NarrativeBlock(
                GOLDEN_BLOCK_ONE,
                GOLDEN_VERSION_ONE,
                OrderKey.rebalanced(0, 2),
                "Kai waits.",
                new BlockMetadata(Map.of(
                        "location", Map.of("mode", "inherited"),
                        "weather", Map.of("mode", "inherited")
                )),
                Map.of()
        );
        NarrativeBlock second = new NarrativeBlock(
                GOLDEN_BLOCK_TWO,
                GOLDEN_VERSION_TWO,
                OrderKey.rebalanced(1, 2),
                "Min enters.",
                new BlockMetadata(Map.of(
                        "weather", Map.of("mode", "explicit", "value", "rain"),
                        "pov", Map.of("mode", "explicit", "character_id", "char_min"),
                        "presence_events", List.of(Map.of(
                                "type", "enter",
                                "character_id", "char_min",
                                "evidence", Map.of(
                                        "start_grapheme", 0,
                                        "end_grapheme", 10,
                                        "quote", "Min enters",
                                        "quote_hash", MIN_ENTERS_HASH
                                )
                        ))
                )),
                Map.of()
        );
        NarrativeScene firstScene = new NarrativeScene(
                GOLDEN_SCENE_ONE,
                GOLDEN_CHAPTER,
                OrderKey.rebalanced(0, 2),
                "Alley",
                TransitionMode.OPENING,
                new SceneSeed(Map.of(
                        "time", Map.of("mode", "explicit", "value", "night"),
                        "location", Map.of(
                                "mode", "explicit", "value", Map.of("id", "alley")
                        ),
                        "weather", Map.of("mode", "unknown"),
                        "present_character_ids", List.of("char_kai")
                )),
                List.of(first, second),
                Map.of()
        );

        NarrativeBlock third = new NarrativeBlock(
                GOLDEN_BLOCK_THREE,
                GOLDEN_VERSION_THREE,
                OrderKey.initial(),
                "Rain stops.",
                new BlockMetadata(Map.of(
                        "time", Map.of("mode", "inherited"),
                        "weather", Map.of("mode", "inherited")
                )),
                Map.of()
        );
        NarrativeScene secondScene = new NarrativeScene(
                GOLDEN_SCENE_TWO,
                GOLDEN_CHAPTER,
                OrderKey.rebalanced(1, 2),
                "Roof",
                TransitionMode.CUT,
                new SceneSeed(Map.of(
                        "time", Map.of("mode", "inherited"),
                        "location", Map.of(
                                "mode", "explicit", "value", Map.of("id", "roof")
                        ),
                        "weather", Map.of("mode", "not_applicable"),
                        "present_character_ids", List.of("char_min")
                )),
                List.of(third),
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                GOLDEN_CHAPTER,
                OrderKey.initial(),
                "Arrival",
                List.of(firstScene, secondScene),
                Map.of()
        );
        return new RevisionManifest(
                GOLDEN_REVISION,
                null,
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(GOLDEN_NOVEL, List.of(chapter), Map.of())
        );
    }

    private static RevisionManifest withScene(
            RevisionManifest revision,
            NarrativeScene scene
    ) {
        NarrativeChapter original = revision.novel().chapters().getFirst();
        NarrativeChapter chapter = new NarrativeChapter(
                original.id(),
                original.orderKey(),
                original.title(),
                List.of(scene),
                original.extensions()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                revision.id(),
                Instant.parse("2026-08-21T12:02:00Z"),
                new NarrativeNovel(revision.novel().id(), List.of(chapter), revision.novel().extensions())
        );
    }

    private static byte[] readFixture(String name) throws IOException {
        try (InputStream input = DeterministicRendererTest.class.getResourceAsStream(
                "/golden/" + name
        )) {
            if (input == null) {
                throw new IOException("Missing renderer fixture " + name);
            }
            return input.readAllBytes();
        }
    }

    private static String id(String prefix, int suffix) {
        return prefix + "_018f0f5e-7b4a-7c00-8000-" + "%012d".formatted(suffix);
    }
}
