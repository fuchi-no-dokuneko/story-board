package dev.storyblock.style;

import java.util.List;

public record StyleAnalysisWindowSlice(
        List<StyleAnalysisWindowFinding> items,
        Integer nextOrdinal
) {
    public StyleAnalysisWindowSlice {
        items = List.copyOf(items);
        if (nextOrdinal != null && nextOrdinal < 0) {
            throw new IllegalArgumentException("Style analysis next ordinal is invalid");
        }
    }
}
