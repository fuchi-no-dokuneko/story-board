package dev.storyblock.style;

public enum StyleStratumKind {
    NARRATION("narration"),
    DIALOGUE("dialogue");

    private final String canonicalName;

    StyleStratumKind(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static StyleStratumKind fromCanonicalName(String value) {
        for (StyleStratumKind kind : values()) {
            if (kind.canonicalName.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unsupported style stratum kind " + value);
    }
}
