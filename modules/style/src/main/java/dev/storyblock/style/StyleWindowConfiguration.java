package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import java.util.Map;
import java.util.Set;

public record StyleWindowConfiguration(
        int operationalGraphemes,
        int operationalStrideGraphemes,
        int microGraphemes,
        int microStrideGraphemes,
        boolean nonOverlapEnabled
) {
    private static final Set<String> FIELDS = Set.of(
            "operational_graphemes", "operational_stride_graphemes",
            "micro_graphemes", "micro_stride_graphemes", "non_overlap_enabled"
    );

    public StyleWindowConfiguration {
        if (operationalGraphemes < 2_000 || operationalGraphemes > 4_000
                || operationalStrideGraphemes < 1
                || operationalStrideGraphemes > operationalGraphemes
                || microGraphemes < 500 || microGraphemes > 1_000
                || microStrideGraphemes < 1 || microStrideGraphemes > microGraphemes) {
            throw new IllegalArgumentException("Style window configuration is out of range");
        }
    }

    public static StyleWindowConfiguration defaults() {
        return new StyleWindowConfiguration(3_000, 1_500, 750, 375, true);
    }

    public static StyleWindowConfiguration fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_window_configuration");
        return new StyleWindowConfiguration(
                StyleCanonical.integer(
                        value, "operational_graphemes", "style_window_configuration"
                ),
                StyleCanonical.integer(
                        value, "operational_stride_graphemes", "style_window_configuration"
                ),
                StyleCanonical.integer(
                        value, "micro_graphemes", "style_window_configuration"
                ),
                StyleCanonical.integer(
                        value, "micro_stride_graphemes", "style_window_configuration"
                ),
                StyleCanonical.bool(
                        value, "non_overlap_enabled", "style_window_configuration"
                )
        );
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "micro_graphemes", microGraphemes,
                "micro_stride_graphemes", microStrideGraphemes,
                "non_overlap_enabled", nonOverlapEnabled,
                "operational_graphemes", operationalGraphemes,
                "operational_stride_graphemes", operationalStrideGraphemes
        ), "style_window_configuration");
    }

    public String configurationHash() {
        return CanonicalJson.hash(canonicalValue());
    }
}
