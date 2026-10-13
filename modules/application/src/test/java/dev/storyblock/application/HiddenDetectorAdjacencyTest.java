package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.*;
import dev.storyblock.detector.*;
import dev.storyblock.renderer.RenderRange;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenDetectorAdjacencyTest {
    @Test void findingsUseVisibleNeighborsWithinScenesAndAcrossBoundaries() {
        var base = HiddenBlockFixture.revision(); var chapter = base.novel().chapters().getFirst();
        var scenes = new ArrayList<NarrativeScene>(chapter.scenes()); var first = scenes.getFirst();
        var blocks = new ArrayList<>(first.blocks()); var changed = blocks.get(3);
        blocks.set(3,changed.revise(changed.text(),new BlockMetadata(Map.of("location",
                Map.of("mode","explicit","value","new"))),changed.extensions()));
        scenes.set(0,new NarrativeScene(first.id(),first.chapterId(),first.orderKey(),first.title(),
                first.transitionMode(),new SceneSeed(Map.of("location",Map.of("mode","explicit","value","old"),
                "present_character_ids",List.of("Lin"))),blocks,first.extensions()));
        var second = scenes.getLast();
        scenes.set(1,second.withInitialMeta(new SceneSeed(Map.of("present_character_ids",List.of("Lin","Mary")))));
        var full = new RevisionManifest(base.id(),null,base.createdAt(),new NarrativeNovel(base.novel().id(),
                List.of(chapter.withScenes(scenes)),base.novel().extensions()));
        var detector = new AdjacentMetadataDetector();
        var result = detector.detect(full,HiddenBlockFixture.HASH,RenderRange.all());
        assertEquals(detector.detect(HiddenBlockFixture.withoutHidden(full),HiddenBlockFixture.HASH,RenderRange.all()),result);
        assertFalse(result.findings().isEmpty());
        var visibleIds = new HashSet<>(NarrativeView.visibleBlocks(full).stream().map(NarrativeBlock::id).toList());
        for (var finding : result.findings()) {
            assertTrue(visibleIds.containsAll(finding.affectedBlockIds()));
            assertTrue(visibleIds.containsAll(finding.contextBlockIds()));
        }
        var boundary = result.findings().stream().filter(f -> f.code()==FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER)
                .findFirst().orElseThrow();
        assertEquals(Set.of(NarrativeView.visibleBlocks(scenes.get(0)).getLast().id(),
                NarrativeView.visibleBlocks(scenes.get(1)).getFirst().id()),new HashSet<>(boundary.affectedBlockIds()));
        assertFalse(CanonicalJson.string(result).contains(HiddenBlockFixture.SENTINEL));
    }
}
