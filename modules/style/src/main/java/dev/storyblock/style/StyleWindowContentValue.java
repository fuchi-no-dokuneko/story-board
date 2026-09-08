package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StyleWindowContentValue {
    static Map<String, Object> contentValue(
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
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_ids", blockIds.stream().map(Ids.BlockId::value).toList());
        value.put("full_sized", fullSized);
        value.put("grapheme_count", graphemeCount);
        value.put("intentional_style_shift_reason", shiftReason);
        value.put("kind", kind.canonicalName());
        value.put("narrative_mode", narrativeMode);
        value.put("pov", pov);
        value.put("requested_stratum", stratum.canonicalValue());
        value.put("segment", segment);
        return value;
    }
}
