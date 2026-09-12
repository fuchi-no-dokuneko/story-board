package dev.storyblock.style;

import java.util.Objects;

final class StyleAnalysisExecutorSameContext {
    static boolean sameContext(
            StyleWindowScore operational,
            StyleWindowScore supporting
    ) {
        StyleWindow expected = operational.window();
        StyleWindow actual = supporting.window();
        return expected.segment() == actual.segment()
                && expected.requestedStratum().equals(actual.requestedStratum())
                && expected.pov().equals(actual.pov())
                && expected.narrativeMode().equals(actual.narrativeMode())
                && Objects.equals(
                        expected.intentionalStyleShiftReason(),
                        actual.intentionalStyleShiftReason()
                )
                && operational.profileSelection().selectedStratum().equals(
                        supporting.profileSelection().selectedStratum()
                );
    }
}
