package dev.storyblock.style;

public record StyleProfileSaveResult(
        StyleProfile profile,
        boolean idempotentReplay
) {
    public StyleProfileSaveResult {
        java.util.Objects.requireNonNull(profile, "profile");
    }
}
