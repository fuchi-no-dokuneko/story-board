package dev.storyblock.style;

public enum StyleDecisionState {
    NORMAL("normal"),
    WARNING("warning"),
    REWRITE_CANDIDATE("rewrite_candidate"),
    TOPIC_SHIFT_ONLY("topic_shift_only"),
    LOW_CONFIDENCE("low_confidence");

    private final String canonicalName;

    StyleDecisionState(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
