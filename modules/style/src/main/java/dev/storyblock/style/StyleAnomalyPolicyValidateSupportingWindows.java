package dev.storyblock.style;

import java.util.List;

final class StyleAnomalyPolicyValidateSupportingWindows {
    static void validateSupportingWindows(
            StyleWindowScore operational,
            List<StyleWindowScore> nonOverlap,
            List<StyleWindowScore> micro
    ) {
        if (nonOverlap.stream().anyMatch(score ->
                !score.window().sustainmentEligible()
                        || !StyleAnomalyPolicySameContext.sameContext(operational, score)
        )) {
            throw new IllegalArgumentException(
                    "Style sustainment inputs must be full non-overlap windows in the same context"
            );
        }
        if (micro.stream().anyMatch(score ->
                !score.window().localizationOnly()
                        || !StyleAnomalyPolicySameContext.sameContext(operational, score)
        )) {
            throw new IllegalArgumentException(
                    "Style localization inputs must be micro windows in the same context"
            );
        }
        if (nonOverlap.stream().map(score -> score.window().windowId())
                .distinct().count() != nonOverlap.size()
                || micro.stream().map(score -> score.window().windowId())
                .distinct().count() != micro.size()) {
            throw new IllegalArgumentException(
                    "Style supporting windows must have unique identities"
            );
        }
        for (int first = 0; first < nonOverlap.size(); first++) {
            for (int second = first + 1; second < nonOverlap.size(); second++) {
                if (nonOverlap.get(first).window().overlaps(
                        nonOverlap.get(second).window()
                )) {
                    throw new IllegalArgumentException(
                            "Style sustainment windows must not overlap"
                    );
                }
            }
        }
    }
}
