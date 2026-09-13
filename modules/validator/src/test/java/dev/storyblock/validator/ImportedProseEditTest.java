package dev.storyblock.validator;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImportedProseEditTest {
    @Test void untouchedProseAllowsCorrection() {
        var base = base();
        var scene = base.novel().chapters().getFirst().scenes().getFirst();
        var corrected = scene.blocks().get(1).revise("雨水敲著窗戶。", BlockMetadata.empty(), Map.of());
        var candidate = replace(base, List.of(scene.blocks().getFirst(), corrected));
        var report = validate(candidate, base);
        assertTrue(report.committable());
        assertEquals(1, report.warnings().size());
        assertEquals(ValidationCode.PRESENCE_EVENT_REQUIRED, report.warnings().getFirst().code());
    }

    @Test void changedPresenceRequiresMetadata() {
        var base = base();
        var scene = base.novel().chapters().getFirst().scenes().getFirst();
        var changed = scene.blocks().getFirst().revise("林雨走進車站。", BlockMetadata.empty(), Map.of());
        var report = validate(replace(base, List.of(changed, scene.blocks().get(1))), base);
        assertFalse(report.committable());
        assertTrue(report.violations().stream().anyMatch(issue -> issue.code() == ValidationCode.PRESENCE_EVENT_REQUIRED));
    }

    private static ValidationReport validate(RevisionManifest candidate, RevisionManifest base) {
        return new DeterministicValidator().validateRevision(candidate, base,
                NarrativeCanonicalMapper.toCanonical(candidate).contentHash());
    }
    private static RevisionManifest base() {
        var chapter = Ids.ChapterId.create();
        var blocks = new ArrayList<NarrativeBlock>();
        for (String text : List.of("林雨走進候車室。", "雨水落在窗邊。"))
            blocks.add(NarrativeBlock.create(Ids.BlockId.create(), OrderKey.rebalanced(blocks.size(), 2), text, BlockMetadata.empty(), Map.of()));
        var scene = new NarrativeScene(Ids.SceneId.create(), chapter, OrderKey.initial(), "候車", TransitionMode.OPENING, SceneSeed.empty(), blocks, Map.of());
        var novel = new NarrativeNovel(Ids.NovelId.create(), List.of(new NarrativeChapter(chapter, OrderKey.initial(), "雨夜", List.of(scene), Map.of())), Map.of());
        return new RevisionManifest(Ids.RevisionId.create(), null, Instant.now(), novel);
    }
    private static RevisionManifest replace(RevisionManifest base, List<NarrativeBlock> blocks) {
        var chapter = base.novel().chapters().getFirst();
        var scene = chapter.scenes().getFirst().withBlocks(blocks);
        var novel = new NarrativeNovel(base.novel().id(), List.of(new NarrativeChapter(chapter.id(), chapter.orderKey(), chapter.title(), List.of(scene), Map.of())), Map.of());
        return new RevisionManifest(Ids.RevisionId.create(), base.id(), Instant.now(), novel);
    }
}
