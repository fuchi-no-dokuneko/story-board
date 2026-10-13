package dev.storyblock.application;

import dev.storyblock.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenFlagToggleTest {
    private final NarrativeEditor editor = new NarrativeEditor(id -> { throw new IllegalArgumentException(); });

    @Test void toggleFlagByReplacementKeepsIdentityAndRequiresVisibleTextOnRemoval() {
        var base = HiddenBlockFixture.revision(); var scene = base.novel().chapters().getFirst().scenes().getFirst();
        var visible = scene.blocks().get(1);
        var hidden = new BlockDraft(visible.id(),"Motive\n"+"象徵".repeat(100),BlockMetadata.empty(),Map.of(BlockHidden.EXTENSION_KEY,true));
        var mark = replace(base,scene,visible,hidden);
        var changed = editor.apply(base,mark,Ids.RevisionId.create(),base.createdAt());
        assertTrue(changed.requireBlock(visible.id()).isHidden());
        assertNotEquals(visible.versionId(),changed.requireBlock(visible.id()).versionId());
        var currentScene = changed.requireScene(scene.id()); var note = changed.requireBlock(visible.id());
        var invalid = replace(changed,currentScene,note,new BlockDraft(note.id(),note.text(),BlockMetadata.empty(),Map.of()));
        assertFalse(new PreviewService(id -> changed).preview(changed,invalid,Ids.RevisionId.create(),base.createdAt()).committable());
        var remove = replace(changed,currentScene,note,new BlockDraft(note.id(),"她明白了。",BlockMetadata.empty(),Map.of()));
        assertFalse(editor.apply(changed,remove,Ids.RevisionId.create(),base.createdAt()).requireBlock(note.id()).isHidden());
    }


    private EditOperation replace(RevisionManifest revision,NarrativeScene scene,NarrativeBlock block,BlockDraft draft) {
        return new EditOperation.ReplaceBlockRange(HiddenBlockFixture.context(revision),
                BlockRangeGuard.capture(scene,block.id(),block.id()),List.of(draft));
    }
}
