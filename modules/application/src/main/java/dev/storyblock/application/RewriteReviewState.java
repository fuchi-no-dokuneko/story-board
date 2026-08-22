package dev.storyblock.application;

public enum RewriteReviewState {
    READY("ready"),
    MANUAL_ONLY("manual_only"),
    REJECTED("rejected"),
    STALE("stale"),
    EXPIRED("expired");

    private final String canonicalName;

    RewriteReviewState(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
