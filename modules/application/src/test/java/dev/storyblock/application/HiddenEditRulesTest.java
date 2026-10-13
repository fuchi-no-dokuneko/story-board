package dev.storyblock.application;

import dev.storyblock.domain.*;
import dev.storyblock.validator.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenEditRulesTest {
    @Test void notesRejectSplitMergeAndExtend() {
        var base = HiddenBlockFixture.revision(); var scene = base.novel().chapters().getFirst().scenes().getFirst();
        var context = HiddenBlockFixture.context(base);
        var note = scene.blocks().getFirst(); var prose = scene.blocks().get(1);
        var hidden = new BlockDraft(prose.id(),prose.text(),BlockMetadata.empty(),Map.of(BlockHidden.EXTENSION_KEY,true));
        var normal = BlockDraft.create("她微笑了。",BlockMetadata.empty());
        var noteGuard = BlockRangeGuard.capture(scene,note.id(),note.id());
        var proseGuard = BlockRangeGuard.capture(scene,prose.id(),prose.id());
        var both = BlockRangeGuard.capture(scene,note.id(),prose.id());
        var operations = List.<EditOperation>of(
                new EditOperation.SplitBlock(context,noteGuard,1,List.of(hidden,normal)),
                new EditOperation.SplitBlock(context,proseGuard,1,List.of(hidden,normal)),
                new EditOperation.MergeBlocks(context,both,normal),
                new EditOperation.MergeBlocks(context,both,hidden),
                new EditOperation.ExtendBlock(context,noteGuard,EditOperation.ExtensionPosition.AFTER,normal),
                new EditOperation.ExtendBlock(context,proseGuard,EditOperation.ExtensionPosition.AFTER,hidden));
        for (var operation : operations) {
            var report = new DeterministicValidator().validateOperation(base,context.expectedHeadHash(),operation);
            assertEquals(ValidationCode.HIDDEN_BLOCK_UNSUPPORTED,report.violations().getFirst().code());
            var preview = new PreviewService(id -> base).preview(base,operation,Ids.RevisionId.create(),base.createdAt());
            assertFalse(preview.committable());
        }
    }

    @Test void correctionOnlyAllowsProvenance() {
        var base = HiddenBlockFixture.revision(); var scene = base.novel().chapters().getFirst().scenes().getFirst();
        var note = scene.blocks().getFirst(); var context = HiddenBlockFixture.context(base);
        var valid = new EditOperation.CorrectBlockMeta(context,scene.id(),BlockVersionRef.from(note),
                new BlockMetadata(Map.of("provenance",Map.of("author","Lin"))));
        assertTrue(new PreviewService(id -> base).preview(base,valid,Ids.RevisionId.create(),base.createdAt()).committable());
        var invalid = new EditOperation.CorrectBlockMeta(context,scene.id(),BlockVersionRef.from(note),
                new BlockMetadata(Map.of("location","station")));
        assertEquals(ValidationCode.HIDDEN_BLOCK_META_NOT_ALLOWED,
                new DeterministicValidator().validateOperation(base,context.expectedHeadHash(),invalid)
                        .violations().getFirst().code());
    }
}
