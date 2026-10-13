package dev.storyblock.application;

import dev.storyblock.domain.*;
import dev.storyblock.validator.DeterministicValidator;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenPriorPresenceTest {
    @Test void insertsAfterLeadingAndInteriorNotesUseNearestVisibleOrSceneSeed() {
        var base = HiddenBlockFixture.revision(); var scene = base.novel().chapters().getFirst().scenes().getFirst();
        var speaker = new BlockMetadata(Map.of("speech",Map.of("direct_speaker_id","Lin")));
        for (int index : List.of(0,2)) {
            var note = new BlockDraft(Ids.BlockId.create(),"Lin left. This is subtext, not an exit.",
                    BlockMetadata.empty(),Map.of(BlockHidden.EXTENSION_KEY,true));
            var prose = BlockDraft.create("「茶涼了。」",speaker);
            var operation = new EditOperation.InsertBlocks(HiddenBlockFixture.context(base),
                    InsertionPoint.after(scene.id(),scene.blocks().get(index).id()),List.of(note,prose));
            var report = new DeterministicValidator().validateOperationCandidates(base,HiddenBlockFixture.HASH,operation);
            assertTrue(report.committable(),report.toString());
            assertTrue(new PreviewService(id -> base).preview(base,operation,Ids.RevisionId.create(),base.createdAt()).committable());
        }
    }
}
