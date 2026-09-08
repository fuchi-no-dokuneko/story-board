package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleWindow(
    String windowId,
    StyleWindowKind kind,
    int segment,
    StyleStratum requestedStratum,
    String pov,
    String narrativeMode,
    List<Ids.BlockId> blockIds,
    int graphemeCount,
    boolean fullSized,
    String intentionalStyleShiftReason
) {
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public StyleWindow {
    Objects.requireNonNull(kind, "kind");
    if (segment < 0) {
      throw new IllegalArgumentException("Style window segment cannot be negative");
    }
    Objects.requireNonNull(requestedStratum, "requestedStratum");
    pov = StyleWindowRequireLabel.requireLabel(pov, "pov");
    narrativeMode = StyleWindowRequireLabel.requireLabel(narrativeMode, "narrativeMode");
    blockIds = List.copyOf(blockIds);
    StyleWindowValidation.validate(windowId, kind, segment, requestedStratum, pov, narrativeMode, blockIds, graphemeCount, fullSized, intentionalStyleShiftReason);
  }

  public static StyleWindow create(
      StyleWindowKind kind,
      int segment,
      StyleStratum requestedStratum,
      String pov,
      String narrativeMode,
      List<Ids.BlockId> blockIds,
      int graphemeCount,
      boolean fullSized,
      String intentionalStyleShiftReason
  ) {
    return StyleWindowCreateFactory.create(kind, segment, requestedStratum, pov, narrativeMode, blockIds, graphemeCount, fullSized, intentionalStyleShiftReason);
  }

  public boolean primaryDecisionEligible() {
    return kind.primaryDecisionWindow() && fullSized;
  }

  public boolean sustainmentEligible() {
    return kind.sustainmentEvidence() && fullSized;
  }

  public boolean localizationOnly() {
    return kind.localizationOnly();
  }

  public boolean overlaps(StyleWindow other) {
    Set<Ids.BlockId> ids = new HashSet<>(blockIds);
    return other.blockIds.stream().anyMatch(ids::contains);
  }

  public Map<String, Object> canonicalValue() {
    return StyleWindowCanonicalValueAction.canonicalValue(this);
  }

}
