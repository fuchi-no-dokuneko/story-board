package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.util.List;

final class StyleWindowCalculateId {
    static String calculateId(
            StyleWindowKind kind,
            int segment,
            StyleStratum stratum,
            String pov,
            String narrativeMode,
            List<Ids.BlockId> blockIds,
            int graphemeCount,
            boolean fullSized,
            String shiftReason
    ) {
        return CanonicalJson.hash(StyleWindowContentValue.contentValue(
                kind,
                segment,
                stratum,
                pov,
                narrativeMode,
                blockIds,
                graphemeCount,
                fullSized,
                shiftReason
        ));
    }
}
