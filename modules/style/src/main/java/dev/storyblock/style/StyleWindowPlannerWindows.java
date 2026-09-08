package dev.storyblock.style;

import dev.storyblock.domain.UnicodeText;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.style.StyleWindowPlanner.Segment;

final class StyleWindowPlannerWindows {
    static List<StyleWindow> windows(
            Segment segment,
            int segmentIndex,
            StyleWindowKind kind,
            int target,
            int stride
    ) {
        List<StyleAnalysisBlock> blocks = segment.blocks();
        int[] prefix = new int[blocks.size() + 1];
        for (int index = 0; index < blocks.size(); index++) {
            prefix[index + 1] = prefix[index]
                    + UnicodeText.graphemeCount(blocks.get(index).block().text());
        }
        List<StyleWindow> result = new ArrayList<>();
        int start = 0;
        while (start < blocks.size()) {
            int end = start;
            while (end < blocks.size() && prefix[end + 1] - prefix[start] < target) {
                end++;
            }
            int exclusiveEnd = Math.min(end + 1, blocks.size());
            int graphemes = prefix[exclusiveEnd] - prefix[start];
            boolean full = graphemes >= target;
            if (full || start == 0) {
                List<StyleAnalysisBlock> members = blocks.subList(start, exclusiveEnd);
                result.add(StyleWindow.create(
                        kind,
                        segmentIndex,
                        StyleWindowPlannerStratum.stratum(segment.key().stratumKind(), members),
                        segment.key().pov(),
                        segment.key().narrativeMode(),
                        members.stream().map(member -> member.block().id()).toList(),
                        graphemes,
                        full,
                        segment.key().shiftReason()
                ));
            }
            if (!full) {
                break;
            }
            int desired = prefix[start] + stride;
            int next = start + 1;
            while (next < blocks.size() && prefix[next] < desired) {
                next++;
            }
            if (next >= blocks.size()) {
                break;
            }
            start = next;
        }
        return List.copyOf(result);
    }
}
