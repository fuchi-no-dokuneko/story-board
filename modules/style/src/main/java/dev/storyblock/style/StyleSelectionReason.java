package dev.storyblock.style;

public enum StyleSelectionReason {
    EXACT("exact"),
    SPEAKER_FALLBACK("speaker_fallback"),
    UNAVAILABLE("unavailable");

    private final String canonicalName;

    StyleSelectionReason(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
