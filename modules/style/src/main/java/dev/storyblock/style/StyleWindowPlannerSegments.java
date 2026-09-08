package dev.storyblock.style;

import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.style.StyleWindowPlanner.Segment;
import static dev.storyblock.style.StyleWindowPlanner.SegmentKey;

final class StyleWindowPlannerSegments {
    static List<Segment> segments(List<StyleAnalysisBlock> blocks) {
        List<Segment> result = new ArrayList<>();
        List<StyleAnalysisBlock> current = new ArrayList<>();
        SegmentKey currentKey = null;
        for (StyleAnalysisBlock block : blocks) {
            SegmentKey key = SegmentKey.from(block);
            if (currentKey != null && !currentKey.equals(key)) {
                result.add(new Segment(currentKey, List.copyOf(current)));
                current.clear();
            }
            currentKey = key;
            current.add(block);
        }
        if (!current.isEmpty()) {
            result.add(new Segment(currentKey, List.copyOf(current)));
        }
        return List.copyOf(result);
    }
}
