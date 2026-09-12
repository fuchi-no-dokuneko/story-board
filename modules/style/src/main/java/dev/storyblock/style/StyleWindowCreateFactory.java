package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.util.List;

final class StyleWindowCreateFactory {
    static StyleWindow create(StyleWindowKind kind, int segment, StyleStratum requestedStratum, String pov, String narrativeMode, List<Ids.BlockId> blockIds, int graphemeCount, boolean fullSized, String intentionalStyleShiftReason)  {
        return new StyleWindow(
                StyleWindowCalculateId.calculateId(
                        kind,
                        segment,
                        requestedStratum,
                        pov,
                        narrativeMode,
                        blockIds,
                        graphemeCount,
                        fullSized,
                        intentionalStyleShiftReason
                ),
                kind,
                segment,
                requestedStratum,
                pov,
                narrativeMode,
                blockIds,
                graphemeCount,
                fullSized,
                intentionalStyleShiftReason
        );
    }
}
