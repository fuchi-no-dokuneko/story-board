package dev.storyblock.monitor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
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
import dev.storyblock.validator.EvidenceSpans;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MonitorPacketFactoryTest {
    private static final String HASH = "sha256:" + "a".repeat(64);

    @Test
    void packetContainsOnlyTargetAndTwoNeighborsOnEachSide() {
        RevisionManifest revision = revision(7);
        List<NarrativeBlock> blocks = revision.liveBlocks();

        MonitorPacket packet = new MonitorPacketFactory().create(
                revision, HASH, blocks.get(3).id(), 2
        );

        assertEquals(
                blocks.subList(1, 6).stream().map(NarrativeBlock::id).toList(),
                packet.renderPacket().blocks().stream().map(block -> block.blockId()).toList()
        );
        assertEquals(5, packet.localInvariants().windowBlocks().size());
        assertEquals(List.of(
                MonitorTool.SUBMIT_FINDING,
                MonitorTool.SUBMIT_PROPOSED_OPERATION
        ), packet.allowedTools());
        assertFalse(packet.canonicalValue().containsKey("raw_database"));
        assertFalse(packet.canonicalValue().containsKey("global_wiki"));
    }

    @Test
    void edgePacketIsBoundedAndRepeatedCreationIsByteIdentical() {
        RevisionManifest revision = revision(7);
        byte[] revisionBefore = NarrativeCanonicalMapper.toCanonical(revision).canonicalBytes();
        MonitorPacketFactory factory = new MonitorPacketFactory();

        MonitorPacket first = factory.create(
                revision, HASH, revision.liveBlocks().getFirst().id(), 2
        );
        MonitorPacket second = factory.create(
                revision, HASH, revision.liveBlocks().getFirst().id(), 2
        );

        assertEquals(3, first.renderPacket().blocks().size());
        assertEquals(
                first.renderPacket().resolvedMetadata().getLast().after(),
                first.renderPacket().sceneBoundaries().getFirst().stateOut()
        );
        assertArrayEquals(
                CanonicalJson.bytes(first.canonicalValue()),
                CanonicalJson.bytes(second.canonicalValue())
        );
        assertArrayEquals(
                revisionBefore,
                NarrativeCanonicalMapper.toCanonical(revision).canonicalBytes()
        );
    }

    @Test
    void outputParserRequiresStrictTextBoundEvidence() {
        String quote = "Block";
        Map<String, Object> evidence = Map.of(
                "block_id", Ids.BlockId.create().value(),
                "start_grapheme", 0,
                "end_grapheme", 5,
                "quote", quote,
                "quote_hash", EvidenceSpans.quoteHash(quote)
        );
        Map<String, Object> value = Map.of(
                "kind", "finding",
                "code", "LOCAL_CONTRADICTION",
                "severity", "warning",
                "message", "The local text conflicts.",
                "evidence", List.of(evidence)
        );

        MonitorFinding finding = (MonitorFinding) MonitorOutput.fromCanonical(value);

        assertTrue(finding.evidence().getFirst().matches("Block text."));
        Map<String, Object> withUnknownField = new java.util.LinkedHashMap<>(value);
        withUnknownField.put("unexpected", true);
        assertThrows(
                IllegalArgumentException.class,
                () -> MonitorOutput.fromCanonical(withUnknownField)
        );
        assertThrows(IllegalArgumentException.class, () -> new MonitorEvidence(
                finding.evidence().getFirst().blockId(),
                0,
                5,
                quote,
                "sha256:" + "0".repeat(64)
        ));
    }

    private static RevisionManifest revision(int blockCount) {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        List<NarrativeBlock> blocks = new ArrayList<>();
        for (int index = 0; index < blockCount; index++) {
            blocks.add(NarrativeBlock.create(
                    Ids.BlockId.create(),
                    OrderKey.rebalanced(index, blockCount),
                    "Block " + index + " has one sentence.",
                    new BlockMetadata(Map.of(
                            "location", Map.of(
                                    "mode", "explicit", "value", "room_" + index
                            )
                    )),
                    Map.of()
            ));
        }
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "Monitor scene",
                TransitionMode.OPENING,
                SceneSeed.empty(),
                blocks,
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId,
                OrderKey.initial(),
                "Monitor chapter",
                List.of(scene),
                Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(), List.of(chapter), Map.of())
        );
    }
}
