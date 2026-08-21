package dev.storyblock.domain;

public enum MetadataValueState {
    EXPLICIT("explicit"),
    INHERITED("inherited"),
    UNKNOWN("unknown"),
    NOT_APPLICABLE("not_applicable");

    private final String canonicalName;

    MetadataValueState(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static MetadataValueState fromCanonicalName(String value) {
        for (MetadataValueState state : values()) {
            if (state.canonicalName.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported metadata value state: " + value);
    }
}
