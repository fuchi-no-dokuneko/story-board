package dev.storyblock.application;

import dev.storyblock.domain.*;
import dev.storyblock.validator.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenMergeDraftTest {
    @Test void mergeOfVisibleSourcesRejectsHiddenOutput() {
        var base = HiddenBlockFixture.withoutHidden(HiddenBlockFixture.revision());
        var scene = base.novel().chapters().getFirst().scenes().getFirst();
        var draft = new BlockDraft(scene.blocks().getFirst().id(),"Unstated motive",BlockMetadata.empty(),
                Map.of(BlockHidden.EXTENSION_KEY,true));
        var operation = new EditOperation.MergeBlocks(HiddenBlockFixture.context(base),
                BlockRangeGuard.capture(scene,scene.blocks().getFirst().id(),scene.blocks().getLast().id()),draft);
        assertEquals(ValidationCode.HIDDEN_BLOCK_UNSUPPORTED,new DeterministicValidator().validateOperation(
                base,operation.context().expectedHeadHash(),operation).violations().getFirst().code());
    }
}
