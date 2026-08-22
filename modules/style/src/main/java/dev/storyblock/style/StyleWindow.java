package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public StyleWindow {
        Objects.requireNonNull(kind, "kind");
        if (segment < 0) {
            throw new IllegalArgumentException("Style window segment cannot be negative");
        }
        Objects.requireNonNull(requestedStratum, "requestedStratum");
        pov = requireLabel(pov, "pov");
        narrativeMode = requireLabel(narrativeMode, "narrativeMode");
        blockIds = List.copyOf(blockIds);
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
        String calculated = calculateId(
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
        return new StyleWindow(
                calculateId(
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
        Map<String, Object> value = contentValue(
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
        value.put("window_id", windowId);
        value.put("localization_only", localizationOnly());
        value.put("primary_decision_eligible", primaryDecisionEligible());
        value.put("sustainment_eligible", sustainmentEligible());
        return CanonicalValues.freezeMap(value, "style_window");
    }

    private static String calculateId(
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
        return CanonicalJson.hash(contentValue(
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

    private static Map<String, Object> contentValue(
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

    private static String requireLabel(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Style window " + field + " is invalid");
        }
        return value;
    }
}
