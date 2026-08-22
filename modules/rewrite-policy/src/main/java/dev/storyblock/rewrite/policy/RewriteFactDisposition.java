package dev.storyblock.rewrite.policy;

public enum RewriteFactDisposition {
    BLOCK("block"),
    MANUAL_ONLY("manual_only");

    private final String canonicalName;

    RewriteFactDisposition(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
