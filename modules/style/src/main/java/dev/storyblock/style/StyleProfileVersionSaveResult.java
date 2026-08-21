package dev.storyblock.style;

public record StyleProfileVersionSaveResult(
        StyleProfileVersionView view,
        boolean idempotentReplay
) {
    public StyleProfileVersionSaveResult {
        java.util.Objects.requireNonNull(view, "view");
    }
}
