package dev.storyblock.storage.sqlite;

public record SqliteWalCheckpoint(
        int busy,
        int logFrames,
        int checkpointedFrames,
        long durationMillis
) {
}
