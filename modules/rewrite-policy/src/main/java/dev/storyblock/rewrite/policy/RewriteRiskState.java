package dev.storyblock.rewrite.policy;

public enum RewriteRiskState {
    SAFE("safe"),
    MANUAL_ONLY("manual_only"),
    BLOCKED("blocked");

    private final String canonicalName;

    RewriteRiskState(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
