package dev.storyblock.storage.sqlite;

public record CheckpointPolicy(int revisionInterval, long replayBytesThreshold) {
    public static final CheckpointPolicy DEFAULT = new CheckpointPolicy(100, 1_048_576);

    public CheckpointPolicy {
        if (revisionInterval < 1) {
            throw new IllegalArgumentException("Checkpoint revision interval must be positive");
        }
        if (replayBytesThreshold < 1) {
            throw new IllegalArgumentException("Checkpoint replay byte threshold must be positive");
        }
    }
}
