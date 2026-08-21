package dev.storyblock.detector;

public enum FindingSeverity {
    ERROR("error"),
    WARNING("warning"),
    INFO("info");

    private final String canonicalName;

    FindingSeverity(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
