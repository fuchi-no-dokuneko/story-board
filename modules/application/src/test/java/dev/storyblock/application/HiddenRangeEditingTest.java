package dev.storyblock.application;

import dev.storyblock.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenRangeEditingTest {
    private final NarrativeEditor editor = new NarrativeEditor(id -> { throw new IllegalArgumentException(); });

    @Test void hiddenBlocksCanBeInsertedDeletedAndMovedWithAllBlockGuards() {
        var base = HiddenBlockFixture.revision(); var scenes = base.novel().chapters().getFirst().scenes();
        var source = scenes.getFirst(); var destination = scenes.getLast(); var note = source.blocks().getFirst();
        var range = BlockRangeGuard.capture(source,note.id(),note.id());
        var move = new EditOperation.MoveBlockRange(HiddenBlockFixture.context(base),range,
                InsertionPoint.endOf(destination.id()),SceneBoundaryContract.capture(source),SceneBoundaryContract.capture(destination));
        var moved = editor.apply(base,move,Ids.RevisionId.create(),base.createdAt());
        assertEquals(note,moved.requireScene(destination.id()).blocks().getLast().moveTo(note.orderKey()));
        var delete = new EditOperation.DeleteBlockRange(HiddenBlockFixture.context(base),range);
        assertEquals(9,editor.apply(base,delete,Ids.RevisionId.create(),base.createdAt()).liveBlocks().size());
        var insert = new EditOperation.InsertBlocks(HiddenBlockFixture.context(base),InsertionPoint.endOf(source.id()),
                List.of(new BlockDraft(Ids.BlockId.create(),"Unstated motive",BlockMetadata.empty(),Map.of(BlockHidden.EXTENSION_KEY,true))));
        assertEquals(11,editor.apply(base,insert,Ids.RevisionId.create(),base.createdAt()).liveBlocks().size());
    }

}
