package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.List;
import static dev.storyblock.style.StyleWindow.*;

final class StyleWindowValidation {
  static void validate(String windowId, StyleWindowKind kind, int segment, StyleStratum requestedStratum, String pov, String narrativeMode, List<Ids.BlockId> blockIds, int graphemeCount, boolean fullSized, String intentionalStyleShiftReason) {
    if (blockIds.isEmpty() || new HashSet<>(blockIds).size() != blockIds.size()) {
          throw new IllegalArgumentException("Style window block IDs must be nonempty and unique");
        }
        if (graphemeCount < 1) {
          throw new IllegalArgumentException("Style window grapheme count must be positive");
        }
        if (intentionalStyleShiftReason != null
            && (intentionalStyleShiftReason.isBlank()
            || intentionalStyleShiftReason.length() > 500)) {
          throw new IllegalArgumentException("Style window shift reason is invalid");
        }
        String calculated = StyleWindowCalculateId.calculateId(
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
        if (windowId == null || !HASH.matcher(windowId).matches()
            || !windowId.equals(calculated)) {
          throw new IllegalArgumentException("Style window ID does not match its content");
        }
  }
}
