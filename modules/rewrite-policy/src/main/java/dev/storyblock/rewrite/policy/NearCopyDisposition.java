package dev.storyblock.rewrite.policy;

public enum NearCopyDisposition {
    BLOCK("block"),
    MANUAL_ONLY("manual_only");

    private final String canonicalName;

    NearCopyDisposition(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
