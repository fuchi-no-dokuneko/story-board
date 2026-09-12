package dev.storyblock.style;

import java.util.Objects;

final class StyleAnomalyPolicySameContext {
    static boolean sameContext(
            StyleWindowScore operational,
            StyleWindowScore supporting
    ) {
        StyleWindow expected = operational.window();
        StyleWindow actual = supporting.window();
        return actual.segment() == expected.segment()
                && actual.requestedStratum().equals(expected.requestedStratum())
                && actual.pov().equals(expected.pov())
                && actual.narrativeMode().equals(expected.narrativeMode())
                && Objects.equals(
                        actual.intentionalStyleShiftReason(),
                        expected.intentionalStyleShiftReason()
                )
                && supporting.profileSelection().selectedStratum().equals(
                        operational.profileSelection().selectedStratum()
                );
    }
}
