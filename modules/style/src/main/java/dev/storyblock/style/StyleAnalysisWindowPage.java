package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record StyleAnalysisWindowPage(
        Ids.StyleAnalysisId analysisId,
        List<StyleAnalysisWindowFinding> items,
        String nextCursor
) {
    public StyleAnalysisWindowPage {
        java.util.Objects.requireNonNull(analysisId, "analysisId");
        items = List.copyOf(items);
        if (nextCursor != null && (nextCursor.isBlank() || nextCursor.length() > 256)) {
            throw new IllegalArgumentException("Style analysis cursor is invalid");
        }
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", analysisId.value());
        value.put("items", items.stream()
                .map(StyleAnalysisWindowFinding::canonicalValue).toList());
        value.put("next_cursor", nextCursor);
        return CanonicalValues.freezeMap(value, "style_analysis_window_page");
    }
}
