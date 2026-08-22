package dev.storyblock.style;

public final class StyleAnalysisResultConflictException extends RuntimeException {
    private final String storedResultHash;
    private final String attemptedResultHash;

    public StyleAnalysisResultConflictException(
            String storedResultHash,
            String attemptedResultHash
    ) {
        super("Style analysis already has a different canonical result");
        this.storedResultHash = storedResultHash;
        this.attemptedResultHash = attemptedResultHash;
    }

    public String storedResultHash() {
        return storedResultHash;
    }

    public String attemptedResultHash() {
        return attemptedResultHash;
    }
}
