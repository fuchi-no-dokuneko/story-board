package dev.storyblock.style;

public final class StyleAnalysisSnapshotConflictException extends RuntimeException {
    private final String currentRevisionHash;

    public StyleAnalysisSnapshotConflictException(String currentRevisionHash) {
        super("Style analysis revision hash does not match canonical storage");
        this.currentRevisionHash = currentRevisionHash;
    }

    public String currentRevisionHash() {
        return currentRevisionHash;
    }
}
