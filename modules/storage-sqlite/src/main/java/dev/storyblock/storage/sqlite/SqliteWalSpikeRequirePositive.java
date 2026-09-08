package dev.storyblock.storage.sqlite;



final class SqliteWalSpikeRequirePositive {
    static void requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
