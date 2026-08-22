package dev.storyblock.style;

public final class StyleAnalysisLeaseConflictException extends RuntimeException {
    private final String currentStatusHash;

    public StyleAnalysisLeaseConflictException(String message, String currentStatusHash) {
        super(message);
        this.currentStatusHash = currentStatusHash;
    }

    public String currentStatusHash() {
        return currentStatusHash;
    }
}
