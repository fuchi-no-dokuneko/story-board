package dev.storyblock.domain;

public enum TransitionMode {
    OPENING("opening"),
    CONTINUOUS("continuous"),
    CUT("cut"),
    TIME_SKIP("time_skip"),
    FLASHBACK("flashback"),
    PARALLEL("parallel");

    private final String canonicalName;

    TransitionMode(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static TransitionMode fromCanonicalName(String value) {
        for (TransitionMode mode : values()) {
            if (mode.canonicalName.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported transition mode: " + value);
    }
}
