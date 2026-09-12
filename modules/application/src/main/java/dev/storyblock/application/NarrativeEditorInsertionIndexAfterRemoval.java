package dev.storyblock.application;

import dev.storyblock.domain.InsertionPoint;
import dev.storyblock.domain.NarrativeBlock;
import java.util.List;

final class NarrativeEditorInsertionIndexAfterRemoval {
    static int insertionIndexAfterRemoval(
            List<NarrativeBlock> retained,
            InsertionPoint point
    ) {
        return switch (point.position()) {
            case START -> 0;
            case END -> retained.size();
            case BEFORE -> NarrativeEditorIndexOf.indexOf(retained, point.anchorBlockId());
            case AFTER -> NarrativeEditorIndexOf.indexOf(retained, point.anchorBlockId()) + 1;
        };
    }
}
