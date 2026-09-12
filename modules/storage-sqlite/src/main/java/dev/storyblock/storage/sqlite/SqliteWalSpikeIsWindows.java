package dev.storyblock.storage.sqlite;



final class SqliteWalSpikeIsWindows {
    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
